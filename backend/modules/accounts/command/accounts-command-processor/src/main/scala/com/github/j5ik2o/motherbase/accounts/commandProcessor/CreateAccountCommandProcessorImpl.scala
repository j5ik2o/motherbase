package com.github.j5ik2o.motherbase.accounts.commandProcessor

import akka.NotUsed
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ ActorSystem, Scheduler }
import akka.stream.scaladsl.Flow
import akka.util.Timeout
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.aggregate.AccountProtocol.{
  CreateAccount,
  CreateAccountFailed,
  CreateAccountReply,
  CreateAccountSucceeded
}
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.aggregate.{ AccountProtocol, AccountRef, AccountRefF }

import scala.concurrent.ExecutionContextExecutor

final class CreateAccountCommandProcessorImpl(accountRef: AccountRef, timeout: Timeout)(
    implicit system: ActorSystem[Nothing]
) extends CreateAccountCommandProcessor {

  private implicit val to: Timeout                  = timeout
  private implicit val ec: ExecutionContextExecutor = system.executionContext

  override def execute: Flow[CreateAccountRequest, CreateAccountResponse, NotUsed] =
    Flow[CreateAccountRequest]
      .mapAsync(1) { req =>
        accountRef.ask[CreateAccountReply](ref => CreateAccount(req.accountId, req.name, req.emailAddress, Some(ref)))
      }.map {
        case res: CreateAccountSucceeded =>
          CreateAccountResponse(res.accountId, None)
        case res: CreateAccountFailed =>
          CreateAccountResponse(res.accountId, Some(res.error))
      }
}
