package com.github.j5ik2o.motherbase.accounts.interfaceAdaptor

import akka.actor.typed.ActorSystem
import akka.stream.Materializer
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.http.SwaggerDocService
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.http.controller.AccountCommandController
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
}
