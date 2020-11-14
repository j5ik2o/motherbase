package com.github.j5ik2o.motherbase.accounts.commandProcessor

import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.AskPattern._
import akka.stream.scaladsl.Flow
import akka.util.Timeout
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.aggregate.AccountProtocol.{
  RenameAccount,
  RenameAccountFailed,
  RenameAccountReply,
  RenameAccountSucceeded
}
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.aggregate.AccountRef

import scala.concurrent.ExecutionContextExecutor

class RenameAccountCommandProcessorImpl(accountRef: AccountRef, timeout: Timeout)(
    implicit system: ActorSystem[Nothing]
) extends RenameAccountCommandProcessor {

  private implicit val to: Timeout                  = timeout
  private implicit val ec: ExecutionContextExecutor = system.executionContext

  override def execute: Flow[RenameAccountRequest, RenameAccountResponse, NotUsed] =
    Flow[RenameAccountRequest]
      .mapAsync(1) { req =>
        accountRef.ask[RenameAccountReply](ref => RenameAccount(req.accountId, req.name, Some(ref)))
      }.map {
        case res: RenameAccountSucceeded =>
          RenameAccountResponse(res.accountId, None)
        case res: RenameAccountFailed =>
          RenameAccountResponse(res.accountId, Some(res.error))
      }
}
