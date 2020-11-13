package com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.http.routes

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.{ ContentTypes, HttpEntity, HttpResponse }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.http.SwaggerDocService
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.http.controller.AccountCommandController

final class Routes(
    val swaggerDocService: SwaggerDocService,
    accountCommandController: AccountCommandController
)(
    implicit system: ActorSystem[Nothing]
) {

  def root: Route =
    cors() {
      RouteLogging.default.httpLogRequestResult {
        pathEndOrSingleSlash {
          get {
            index()
          }
        } ~ path("swagger") {
          getFromResource("swagger/index.html")
        } ~ getFromResourceDirectory("swagger") ~
        swaggerDocService.routes ~ accountCommandController.toRoutes
      }
    }

  def index(): Route = complete(
    HttpResponse(
      entity = HttpEntity(
        ContentTypes.`text/html(UTF-8)`,
        """<span>Wellcome to MotherBase API</span><br/><a href="http://localhost:18080/swagger/index.html">swagger</a>"""
      )
    )
  )

}
