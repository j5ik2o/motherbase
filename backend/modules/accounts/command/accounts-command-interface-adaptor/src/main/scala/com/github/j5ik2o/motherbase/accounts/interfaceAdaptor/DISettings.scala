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
import com.github.j5ik2o.motherbase.interfaceAdaptor.grpc.proto.AccountCommandService
import wvlet.airframe._

import scala.concurrent.duration._

object DISettings {

  private[interfaceAdaptor] def designOfActorSystem(
      clusterMode: Boolean,
      system: ActorSystem[Nothing],
      materializer: Materializer
  ): Design = {
    val base = newDesign
      .bind[ActorSystem[Nothing]].toInstance(system)
      .bind[Materializer].toInstance(materializer)
    if (clusterMode) {
      base
        .bind[Cluster].toInstance(Cluster(system))
        .bind[ClusterSharding].toInstance(ClusterSharding(system))
    } else
      base
  }

  private[interfaceAdaptor] def designOfSwagger(host: String, port: Int): Design =
    newDesign
      .bind[SwaggerDocService].toInstance(
        new SwaggerDocService(host, port, Set(classOf[AccountCommandController]))
      )

  private[interfaceAdaptor] def designOfGRPCServices(): Design = {
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

  private[interfaceAdaptor] def designOfHttpControllers(): Design = {
    newDesign.bind[AccountCommandController].toProvider[CreateAccountCommandProcessor, CreateAccountJsonResponder] {
      case (processor, responder) =>
        new AccountCommandControllerImpl(processor, responder)
    }
  }

  private[interfaceAdaptor] def designOfResponders() = {
    newDesign.bind[CreateAccountJsonResponder].toInstance(new CreateAccountJsonResponderImpl)
  }

}
