package com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.aggregate

import akka.actor.typed.ActorRef
import com.github.j5ik2o.motherbase.accounts.domain.accounts.{ AccountId, AccountName, EmailAddress }
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.aggregate.AccountProtocol._
import com.github.j5ik2o.motherbase.interfaceAdaptor.actor.ActorSpec

import scala.concurrent.duration.FiniteDuration

trait SystemAccountAggregateSpecHelper { this: ActorSpec =>

  def createSystemAccount(ref: ActorRef[AccountProtocol.Command], maxDuration: FiniteDuration)(
      systemAccountId: AccountId,
      name: AccountName,
      emailAddress: EmailAddress
  ): CreateAccountReply = {
    val replyProbe = testKit.createTestProbe[CreateAccountReply]()
    ref ! CreateAccount(systemAccountId, name, emailAddress, Some(replyProbe.ref))
    replyProbe.expectMessageType[CreateAccountReply](maxDuration)
  }

  def destroySystemAccount(ref: ActorRef[AccountProtocol.Command], maxDuration: FiniteDuration)(
      systemAccountId: AccountId
  ): DestroyAccountReply = {
    val replyProbe = testKit.createTestProbe[DestroyAccountReply]()
    ref ! DestroyAccount(systemAccountId, Some(replyProbe.ref))
    replyProbe.expectMessageType[DestroyAccountReply](maxDuration)
  }

  def getSystemAccountName(ref: ActorRef[AccountProtocol.Command], maxDuration: FiniteDuration)(
      systemAccountId: AccountId
  ): GetAccountNameReply = {
    val replyProbe = testKit.createTestProbe[GetAccountNameReply]()
    ref ! GetAccountName(systemAccountId, replyProbe.ref)
    replyProbe.expectMessageType[GetAccountNameReply](maxDuration)

  }

}
