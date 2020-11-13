package com.github.j5ik2o.motherbase.accounts.interfaceAdaptor

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter._
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.cluster.typed.Cluster
import akka.stream.Materializer
import com.github.j5ik2o.motherbase.accounts.commandProcessor.CreateAccountCommandProcessor
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.aggregate.{
  AccountAggregate,
  AccountAggregates,
  AccountRef,
  ShardedAccountAggregates
}
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.grpc.service.AccountCommandServiceImpl
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.http.SwaggerDocService
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.http.controller.{
  AccountCommandController,
  AccountCommandControllerImpl
}
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.http.responder.{
  CreateAccountGRPCResponder,
  CreateAccountJsonResponder,
  CreateAccountJsonResponderImpl
}
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.http.routes.Routes
import com.github.j5ik2o.motherbase.interfaceAdaptor.grpc.proto.AccountCommandService
import wvlet.airframe._

import scala.concurrent.duration._

object DISettings {

  def designForGRPCServer(clusterMode: Boolean, system: ActorSystem[Nothing]): Design = {
    designOfActorSystem(clusterMode, system)
      .add(designOfAggregates(clusterMode))
      .add(designOfGRPCServices)
      .add(designOfResponders)
  }

  def designForHttpServer(clusterMode: Boolean, system: ActorSystem[Nothing], host: String, port: Int): Design = {
    designOfActorSystem(clusterMode, system)
      .add(designOfAggregates(clusterMode))
      .add(designOfSwagger(host, port))
      .add(designOfHttpControllers)
      .add(designOfRoutes)
      .add(designOfResponders)
  }

  private[interfaceAdaptor] def designOfActorSystem(
      clusterMode: Boolean,
      system: ActorSystem[Nothing]
  ): Design = {
    val base = newDesign
      .bind[ActorSystem[Nothing]].toInstance(system)
    if (clusterMode) {
      base
        .bind[Cluster].toInstance(Cluster(system))
        .bind[ClusterSharding].toInstance(ClusterSharding(system))
    } else
      base
  }

  def designOfRoutes: Design =
    newDesign.bind[Routes].toEagerSingleton

  private[interfaceAdaptor] def designOfSwagger(host: String, port: Int): Design =
    newDesign
      .bind[SwaggerDocService].toInstance(
        new SwaggerDocService(host, port, Set(classOf[AccountCommandController]))
      )

  private[interfaceAdaptor] def designOfGRPCServices: Design = {
    newDesign
      .bind[AccountCommandService].toProvider[CreateAccountCommandProcessor, CreateAccountGRPCResponder, ActorSystem[
        Nothing
      ]] {
        case (processor, responder, system) =>
          new AccountCommandServiceImpl(processor, responder)(system)
      }
  }

  private[interfaceAdaptor] def designOfAggregates(clusterMode: Boolean) = {
    if (clusterMode)
      newDesign.bind[AccountRef].toProvider[ActorSystem[Nothing], ClusterSharding] {
        case (system, clusterSharding) =>
          ShardedAccountAggregates.initClusterSharding(
            clusterSharding,
            AccountAggregates(_.value.asString)(AccountAggregate(_)),
            Some(10 seconds)
          )
          val b = ShardedAccountAggregates.ofProxy(clusterSharding)
          system.toClassic.spawn(b, "account")
      }
    else
      newDesign.bind[AccountRef].toProvider[ActorSystem[Nothing]] {
        case (system) =>
          val b =
            AccountAggregates(_.value.asString)(AccountAggregate(_))
          system.toClassic.spawn(b, "account")

      }
  }

  private[interfaceAdaptor] def designOfHttpControllers: DesignWithContext[AccountCommandController] = {
    newDesign.bind[AccountCommandController].toProvider[CreateAccountCommandProcessor, CreateAccountJsonResponder] {
      case (processor, responder) =>
        new AccountCommandControllerImpl(processor, responder)
    }
  }

  private[interfaceAdaptor] def designOfResponders: DesignWithContext[CreateAccountJsonResponder] = {
    newDesign.bind[CreateAccountJsonResponder].toInstance(new CreateAccountJsonResponderImpl)
  }

}
