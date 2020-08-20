package com.github.j5ik2o.motherbase.accounts.interfaceAdaptor

import akka.actor.typed.ActorSystem
import akka.stream.Materializer
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.grpc.service.AccountCommandServiceImpl
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.http.SwaggerDocService
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.http.controller.{
  AccountCommandController,
  AccountCommandControllerImpl
}
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.http.responder.{
  CreateAccountGRPCResponder,
  CreateAccountJsonResponder
}
import com.github.j5ik2o.motherbase.commandProcessor.CreateAccountCommandProcessor
import com.github.j5ik2o.motherbase.interfaceAdaptor.grpc.proto.AccountCommandService
import wvlet.airframe._

object DISettings {

  private[interfaceAdaptor] def designOfActorSystem(system: ActorSystem[Nothing], materializer: Materializer): Design =
    newDesign
      .bind[ActorSystem[Nothing]].toInstance(system)
      .bind[Materializer].toInstance(materializer)

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

  private[interfaceAdaptor] def designOfHttpControllers(): Design = {
    newDesign.bind[AccountCommandController].toProvider[CreateAccountCommandProcessor, CreateAccountJsonResponder] {
      case (processor, responder) =>
        new AccountCommandControllerImpl(processor, responder)
    }
  }

}
