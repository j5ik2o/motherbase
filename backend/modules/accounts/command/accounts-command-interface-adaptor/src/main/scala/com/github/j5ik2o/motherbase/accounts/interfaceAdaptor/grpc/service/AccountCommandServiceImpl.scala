package com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.grpc.service

import akka.actor.typed.ActorSystem
import akka.stream.scaladsl.{ Sink, Source }
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.grpc.validate.ValidateSupport
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.grpc.validate.ValidateSupport._
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.http.responder.CreateAccountGRPCResponder
import com.github.j5ik2o.motherbase.accounts.commandProcessor.CreateAccountCommandProcessor
import com.github.j5ik2o.motherbase.interfaceAdaptor.grpc.proto._
import com.github.j5ik2o.motherbase.interfaceAdaptor.grpc.proto.{ CreateAccountResponse => GRPCCreateAccountResponse }

import scala.concurrent.Future

final class AccountCommandServiceImpl(
    createAccountCommandProcessor: CreateAccountCommandProcessor,
    createAccountGrpcResponder: CreateAccountGRPCResponder
)(implicit system: ActorSystem[Nothing])
    extends AccountCommandService
    with ValidateSupport {

  override def createSystemAccount(in: CreateAccountRequest): Future[CreateAccountResponse] = {
    validateRequest(in).fold(
      errors => Future.successful(GRPCCreateAccountResponse("", errors.map { error => Error(error.message) }.toList)), {
        commandRequest =>
          Source
            .single(commandRequest)
            .via(createAccountCommandProcessor.execute)
            .via(createAccountGrpcResponder.response)
            .runWith(Sink.head)
      }
    )
  }

  override def renameSystemAccount(in: RenameAccountRequest): Future[RenameAccountResponse] = ???

  override def destroySystemAccount(in: CreateAccountRequest): Future[CreateAccountResponse] = ???
}
