package com.github.j5ik2o.motherbase.bootstrap

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter._
import akka.actor.typed.{ ActorSystem, PostStop }
import akka.cluster.ClusterEvent.ClusterDomainEvent
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.cluster.typed.{ Cluster, SelfUp, Subscribe }
import akka.http.scaladsl.Http
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
  ShardedAccountAggregates
}
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.http.SwaggerDocService
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.http.controller.{
  AccountCommandController,
  AccountCommandControllerImpl
}
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.http.responder.{
  CreateAccountJsonResponderImpl,
  RenameAccountJsonResponderImpl
}
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.http.routes.Routes
import com.typesafe.config.ConfigFactory
import kamon.Kamon
import org.slf4j.LoggerFactory
import org.slf4j.bridge.SLF4JBridgeHandler

import scala.concurrent.Await
import scala.concurrent.duration._

object Main extends App {

  val logger = LoggerFactory.getLogger(Main.getClass)

  SLF4JBridgeHandler.install()
  val config = ConfigFactory.load()
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

  val mainBehavior = Behaviors.setup[Any] { ctx =>
    val host        = config.getString("host")
    val port        = config.getInt("port")
    val clusterMode = false

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
    } else {
      AccountAggregates(_.value.asString)(AccountAggregate(_))
    }
    implicit val to = Timeout(10 seconds)

    val accountRef = ctx.spawn(accountBehavior, "accounts")

    val swaggerDocService             = new SwaggerDocService(host, port, Set(classOf[AccountCommandController]))
    val createAccountCommandProcessor = new CreateAccountCommandProcessorImpl(accountRef, to)(ctx.system)
    val createAccountJsonResponder    = new CreateAccountJsonResponderImpl
    val renameAccountCommandProcessor = new RenameAccountCommandProcessorImpl(accountRef, to)(ctx.system)
    val renameAccountJsonResponder    = new RenameAccountJsonResponderImpl

    val controller = new AccountCommandControllerImpl(
      createAccountCommandProcessor,
      createAccountJsonResponder,
      renameAccountCommandProcessor,
      renameAccountJsonResponder
    )

    val routes = new Routes(swaggerDocService, controller)(ctx.system).root

    implicit val s = ctx.system.toClassic
    import ctx.executionContext
    val bindingFuture = Http().bindAndHandle(routes, host, port).map { serverBinding =>
      ctx.log.info(s"Server online at ${serverBinding.localAddress}")
      serverBinding
    }

    Behaviors.receiveSignal {
      case (_, PostStop) =>
        val future = bindingFuture.flatMap { serverBinding => serverBinding.unbind() }
        Await.result(future, Duration.Inf)
        Behaviors.same
    }
  }

  ActorSystem(mainBehavior, "main")

}
