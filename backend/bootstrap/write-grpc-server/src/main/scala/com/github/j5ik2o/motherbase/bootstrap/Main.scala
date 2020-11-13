package com.github.j5ik2o.motherbase.bootstrap

import akka.actor.CoordinatedShutdown
import akka.actor.typed.{ ActorSystem, Behavior, PostStop }
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.ClusterEvent.ClusterDomainEvent
import akka.cluster.typed.{ Cluster, Join, SelfUp, Subscribe }
import akka.http.scaladsl.Http
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.scaladsl.AkkaManagement
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.DISettings
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.http.routes.Routes
import com.typesafe.config.ConfigFactory
import kamon.Kamon
import org.slf4j.LoggerFactory
import org.slf4j.bridge.SLF4JBridgeHandler
import akka.actor.typed.scaladsl.adapter._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Main extends App {

  val logger = LoggerFactory.getLogger(Main.getClass)

  SLF4JBridgeHandler.install()
  val config = ConfigFactory.load()
  Kamon.init(config)

  val clusterMode = false

  val mainBehavior = Behaviors.setup[Any] { ctx =>
    val host    = config.getString("host")
    val port    = config.getInt("port")
    val design  = DISettings.designForGRPCServer(clusterMode, ctx.system)
    val session = design.newSession;
    session.start

    AkkaManagement(ctx.system).start()
    ClusterBootstrap(ctx.system).start()

    val cluster: Cluster = Cluster(ctx.system)

    val clusterWatcherBehavior = Behaviors.setup[ClusterDomainEvent] { ctx =>
      cluster.subscriptions ! Subscribe(ctx.self, classOf[ClusterDomainEvent])
      Behaviors.receiveMessage[ClusterDomainEvent] {
        case SelfUp(currentClusterState) =>
          ctx.log.info(s"Cluster ${cluster.selfMember.address} >>> " + currentClusterState)
          Behaviors.same
      }
    }
    ctx.spawn(clusterWatcherBehavior, "cluster-watcher")
    val routes     = session.build[Routes].root
    implicit val s = ctx.system.toClassic
    import ctx.executionContext
    val bindingFuture = Http().bindAndHandle(routes, host, port).map { serverBinding =>
      ctx.log.info(s"Server online at ${serverBinding.localAddress}")
      serverBinding
    }

    Behaviors.receiveSignal {
      case (_, PostStop) =>
        val future = bindingFuture.flatMap { serverBinding =>
          session.shutdown
          serverBinding.unbind()
        }
        Await.result(future, Duration.Inf)
        Behaviors.same
    }
  }

  ActorSystem(mainBehavior, "main")

}
