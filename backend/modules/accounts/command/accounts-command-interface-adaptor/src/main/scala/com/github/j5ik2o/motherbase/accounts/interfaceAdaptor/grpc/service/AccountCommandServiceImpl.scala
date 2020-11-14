package com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.grpc.service

import akka.actor.typed.ActorSystem
import akka.stream.scaladsl.{ Sink, Source }
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.grpc.validate.ValidateSupport
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.grpc.validate.ValidateSupport._
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.http.responder.{
  CreateAccountGRPCResponder,
  RenameAccountGRPCResponder
}
import com.github.j5ik2o.motherbase.accounts.commandProcessor.{
  CreateAccountCommandProcessor,
  RenameAccountCommandProcessor
}
import com.github.j5ik2o.motherbase.interfaceAdaptor.grpc.proto._
import com.github.j5ik2o.motherbase.interfaceAdaptor.grpc.proto.{ CreateAccountResponse => GRPCCreateAccountResponse }
import com.github.j5ik2o.motherbase.interfaceAdaptor.grpc.proto.{ RenameAccountResponse => GRPCRenameAccountResponse }

import scala.concurrent.Future

final class AccountCommandServiceImpl(
    createAccountCommandProcessor: CreateAccountCommandProcessor,
    createAccountGrpcResponder: CreateAccountGRPCResponder,
    renameAccountCommandProcessor: RenameAccountCommandProcessor,
    renameAccountGrpcResponder: RenameAccountGRPCResponder
)(implicit system: ActorSystem[Nothing])
    extends AccountCommandService
    with ValidateSupport {

  override def createAccount(in: CreateAccountRequest): Future[CreateAccountResponse] = {
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

  override def renameAccount(in: RenameAccountRequest): Future[RenameAccountResponse] = {
    validateRequest(in).fold(
      errors => Future.successful(GRPCRenameAccountResponse("", errors.map { error => Error(error.message) }.toList)),
      commandRequest =>
        Source
          .single(commandRequest)
          .via(renameAccountCommandProcessor.execute)
          .via(renameAccountGrpcResponder.response)
          .runWith(Sink.head)
    )

  }

  override def destroyAccount(in: CreateAccountRequest): Future[CreateAccountResponse] = ???
}
