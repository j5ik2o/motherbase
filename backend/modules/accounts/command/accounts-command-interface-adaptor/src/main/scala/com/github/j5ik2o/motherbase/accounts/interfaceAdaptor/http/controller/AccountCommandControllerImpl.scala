package com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.http.controller

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.{ Sink, Source }
import com.github.j5ik2o.motherbase.accounts.commandProcessor.{
  CreateAccountCommandProcessor,
  RenameAccountCommandProcessor
}
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.http.directives.AccountValidateDirectives
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.http.directives.AccountValidateDirectives._
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.http.exceptions.ExceptionHandlers
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.http.json.{
  CreateAccountRequestJson,
  RenameAccountRequestJson
}
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.http.rejections.RejectionHandlers
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.http.responder.{
  CreateAccountJsonResponder,
  RenameAccountJsonResponder
}
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._

final class AccountCommandControllerImpl(
    createAccountCommandProcessor: CreateAccountCommandProcessor,
    createAccountJsonResponder: CreateAccountJsonResponder,
    renameAccountCommandProcessor: RenameAccountCommandProcessor,
    renameAccountJsonResponder: RenameAccountJsonResponder
) extends AccountCommandController
    with AccountValidateDirectives {

  override def toRoutes: Route =
    handleExceptions(ExceptionHandlers.default) {
      handleRejections(RejectionHandlers.default) {
        pathPrefix("v1") {
          createAccount
        }
      }
    }

  override private[controller] def createAccount: Route =
    path("accounts" / "create") {
      post {
        extractMaterializer { implicit mat =>
          entity(as[CreateAccountRequestJson]) { json =>
            validateRequest(json).apply { commandRequest =>
              val responseFuture = Source
                .single(commandRequest)
                .via(createAccountCommandProcessor.execute)
                .via(createAccountJsonResponder.response)
                .runWith(Sink.head)
              onSuccess(responseFuture) { response => complete(response) }
            }
          }
        }
      }
    }

  override private[controller] def renameAccount: Route =
    path("accounts" / Segment) { id =>
      post {
        extractMaterializer { implicit mat =>
          entity(as[RenameAccountRequestJson]) { json =>
            validateRequest(json).apply { commandRequest =>
              val responseFuture = Source
                .single(commandRequest)
                .via(renameAccountCommandProcessor.execute)
                .via(renameAccountJsonResponder.response)
                .runWith(Sink.head)
              onSuccess(responseFuture) { response => complete(response) }
            }
          }
        }
      }
    }
}
