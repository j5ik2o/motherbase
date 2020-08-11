package com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.http.controller

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.{ Sink, Source }
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.http.directives.AccountValidateDirectives
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.http.directives.AccountValidateDirectives._
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.http.json.CreateAccountRequestJson
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.http.responder.CreateAccountResponder
import com.github.j5ik2o.motherbase.commandProcessor.CreateAccountCommandProcessor
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._

final class AccountCommandControllerImpl(
    createAccountCommandProcessor: CreateAccountCommandProcessor,
    createAccountResponder: CreateAccountResponder
) extends AccountCommandController
    with AccountValidateDirectives {

  override def toRoutes: Route = pathPrefix("v1") {
    createAccount
  }

  override private[controller] def createAccount =
    path("accounts" / "create") {
      post {
        extractMaterializer { implicit mat =>
          entity(as[CreateAccountRequestJson]) { json =>
            validateJsonRequest(json).apply { commandRequest =>
              val responseFuture = Source
                .single(commandRequest)
                .via(createAccountCommandProcessor.execute)
                .via(createAccountResponder.response)
                .runWith(Sink.head)
              onSuccess(responseFuture) { response => complete(response) }
            }
          }
        }
      }
    }
}
