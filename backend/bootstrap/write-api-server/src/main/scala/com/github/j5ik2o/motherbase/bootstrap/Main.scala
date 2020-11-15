package com.github.j5ik2o.motherbase.bootstrap

import akka.actor.typed.scaladsl.{ ActorContext, Behaviors }
import akka.actor.typed.scaladsl.adapter._
import akka.actor.typed.{ ActorRef, ActorSystem, Behavior, PostStop }
import akka.cluster.ClusterEvent.ClusterDomainEvent
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.cluster.typed.{ Cluster, SelfUp, Subscribe }
import akka.grpc.scaladsl.{ ServerReflection, ServiceHandler }
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ HttpRequest, HttpResponse }
import akka.http.scaladsl.server.Route
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.scaladsl.AkkaManagement
import akka.util.Timeout
import com.github.j5ik2o.motherbase.accounts.commandProcessor.{
  CreateAccountCommandProcessorImpl,
  RenameAccountCommandProcessorImpl
}
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.aggregate.{
  AccountAggregate,
  AccountAggregates,
  AccountProtocol,
  ShardedAccountAggregates
}
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.grpc.service.AccountCommandServiceImpl
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.http.SwaggerDocService
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.http.controller.{
  AccountCommandController,
  AccountCommandControllerImpl
}
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.http.responder.{
  CreateAccountGRPCResponderImpl,
  CreateAccountJsonResponderImpl,
  RenameAccountGRPCResponderImpl,
  RenameAccountJsonResponderImpl
}
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.http.routes.Routes
import com.github.j5ik2o.motherbase.bootstrap.Main.rest
import com.github.j5ik2o.motherbase.interfaceAdaptor.grpc.proto.{ AccountCommandService, AccountCommandServiceHandler }
import com.typesafe.config.ConfigFactory
import kamon.Kamon
import org.slf4j.LoggerFactory
import org.slf4j.bridge.SLF4JBridgeHandler

import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._
import net.ceedubs.ficus.Ficus._

object Main extends App {

  val logger = LoggerFactory.getLogger(Main.getClass)

  SLF4JBridgeHandler.install()
  val config = ConfigFactory.parseString("""
      |akka.http.server.preview.enable-http2 = on
      |""".stripMargin).withFallback(ConfigFactory.load())
  Kamon.init(config)

  val clusterMode = false

  val clusterWatcherBehavior = Behaviors.setup[ClusterDomainEvent] { ctx =>
    val cluster: Cluster = Cluster(ctx.system)
    cluster.subscriptions ! Subscribe(ctx.self, classOf[ClusterDomainEvent])
    Behaviors.receiveMessage[ClusterDomainEvent] {
      case SelfUp(currentClusterState) =>
        ctx.log.info(s"Cluster ${cluster.selfMember.address} >>> " + currentClusterState)
        Behaviors.same
    }
  }

  def grpc(
      host: String,
      port: Int,
      ctx: ActorContext[_],
      accountRef: ActorRef[AccountProtocol.Command]
  ): Future[Http.ServerBinding] = {
    implicit val s                    = ctx.system.toClassic
    implicit val to                   = Timeout(10 seconds)
    val serverReflection              = ServerReflection.partial(List(AccountCommandService))
    val createAccountCommandProcessor = new CreateAccountCommandProcessorImpl(accountRef, to)(ctx.system)
    val renameAccountCommandProcessor = new RenameAccountCommandProcessorImpl(accountRef, to)(ctx.system)
    val createAccountGRPCResponder    = new CreateAccountGRPCResponderImpl
    val renameAccountGRPCResponder    = new RenameAccountGRPCResponderImpl
    val accountCommandService = new AccountCommandServiceImpl(
      createAccountCommandProcessor,
      createAccountGRPCResponder,
      renameAccountCommandProcessor,
      renameAccountGRPCResponder
    )(ctx.system)
    val accountServiceHandler = AccountCommandServiceHandler.partial(accountCommandService)
    val serviceHandler: HttpRequest => Future[HttpResponse] =
      ServiceHandler.concatOrNotFound(serverReflection, accountServiceHandler)
    import ctx.executionContext
    Http().newServerAt(host, port).bind(serviceHandler).map { binding =>
      ctx.log.info(s"Started [${ctx.system}], grpc = $host, $port")
      binding
    }
  }

  def rest(
      host: String,
      port: Int,
      ctx: ActorContext[_],
      accountRef: ActorRef[AccountProtocol.Command]
  ): Future[Http.ServerBinding] = {
    implicit val s  = ctx.system.toClassic
    implicit val to = Timeout(10 seconds)

    val swaggerDocService             = new SwaggerDocService(host, port, Set(classOf[AccountCommandController]))
    val createAccountCommandProcessor = new CreateAccountCommandProcessorImpl(accountRef, to)(ctx.system)
    val renameAccountCommandProcessor = new RenameAccountCommandProcessorImpl(accountRef, to)(ctx.system)
    val createAccountJsonResponder    = new CreateAccountJsonResponderImpl
    val renameAccountJsonResponder    = new RenameAccountJsonResponderImpl

    val controller = new AccountCommandControllerImpl(
      createAccountCommandProcessor,
      createAccountJsonResponder,
      renameAccountCommandProcessor,
      renameAccountJsonResponder
    )

    val routes: Route = new Routes(swaggerDocService, controller)(ctx.system).root

    import ctx.executionContext
    Http()
      .newServerAt(host, port).bind(routes)
      .map { serverBinding =>
        ctx.log.info(s"Server online at ${serverBinding.localAddress}")
        serverBinding
      }
  }

  val mainBehavior = Behaviors.setup[Any] { ctx =>
    import ctx.executionContext
    val host        = config.getString("host")
    val httpPortOpt = config.getAs[Int]("http-port")
    val grpcPortOpt = config.getAs[Int]("grpc-port")

    val accountBehavior: Behavior[AccountProtocol.Command] = newAccountBehavior(ctx)
    val accountRef: ActorRef[AccountProtocol.Command]      = ctx.spawn(accountBehavior, "accounts")

    val bindingsFuture = (httpPortOpt, grpcPortOpt) match {
      case (Some(httpPort), Some(grpcPort)) =>
        for {
          restBinding <- rest(host, httpPort, ctx, accountRef)
          grpcBinding <- grpc(host, grpcPort, ctx, accountRef)
        } yield Seq(restBinding, grpcBinding)
      case (Some(httpPort), None) =>
        for {
          restBinding <- rest(host, httpPort, ctx, accountRef)
        } yield Seq(restBinding)
      case (None, Some(grpcPort)) =>
        for {
          grpcBinding <- grpc(host, grpcPort, ctx, accountRef)
        } yield Seq(grpcBinding)
      case _ =>
        throw new IllegalArgumentException()
    }

    Behaviors.receiveSignal {
      case (_, PostStop) =>
        val future = bindingsFuture.flatMap(Future.traverse(_)(_.unbind()))
        Await.result(future, Duration.Inf)
        Behaviors.same
    }
  }

  private def newAccountBehavior(ctx: ActorContext[Any]): Behavior[AccountProtocol.Command] = {
    val accountBehavior = if (clusterMode) {
      AkkaManagement(ctx.system).start()
      ClusterBootstrap(ctx.system).start()

      ctx.spawn(clusterWatcherBehavior, "cluster-watcher")

      val clusterSharding = ClusterSharding(ctx.system)
      ShardedAccountAggregates.initClusterSharding(
        clusterSharding,
        AccountAggregates(_.value.asString)(AccountAggregate(_)),
        Some(10 seconds)
      )

      ShardedAccountAggregates.ofProxy(clusterSharding)
    } else
      AccountAggregates(_.value.asString)(AccountAggregate(_))

    accountBehavior
  }

  ActorSystem(mainBehavior, "main")

}
