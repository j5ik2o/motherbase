package com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.aggregate

import akka.actor.typed.{ ActorRef, Behavior }
import com.github.j5ik2o.motherbase.accounts.domain.accounts.{ AccountId, AccountName, EmailAddress }
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.aggregate.AccountProtocol.{
  CreateAccountSucceeded,
  DestroyAccountSucceeded,
  GetAccountNameSucceeded
}
import com.github.j5ik2o.motherbase.interfaceAdaptor.actor.ActorSpec
import org.scalatest.freespec.AnyFreeSpecLike

trait AccountAggregateSpecScenario
    extends AnyFreeSpecLike
    with AggregateSpecScenarioBase
    with AccountAggregateSpecHelper {
  this: ActorSpec =>

  def behavior(systemAccountId: AccountId): Behavior[AccountProtocol.Command]

  def actorRef(systemAccountId: AccountId): ActorRef[AccountProtocol.Command] =
    spawn(behavior(systemAccountId))

  "SystemAccountAggregate" - {
    "create with killActor" in {
      val systemAccountId = AccountId()
      val name            = AccountName("test")
      val emailAddress    = EmailAddress("test@test.com")
      val actorRef1       = actorRef(systemAccountId)

      val createSystemAccountReply = createAccount(actorRef1, maxDuration)(systemAccountId, name, emailAddress)
        .asInstanceOf[CreateAccountSucceeded]
      createSystemAccountReply.accountId shouldBe systemAccountId

      killActors(actorRef1)(maxDuration)

      val actorRef2 = actorRef(systemAccountId)

      val getSystemAccountNameReply =
        getAccountName(actorRef2, maxDuration)(systemAccountId).asInstanceOf[GetAccountNameSucceeded]
      getSystemAccountNameReply.name shouldBe name
    }
    "destroy" in {
      val systemAccountId = AccountId()
      val name            = AccountName("test")
      val emailAddress    = EmailAddress("test@test.com")
      val actorRef1       = actorRef(systemAccountId)

      val createSystemAccountReply = createAccount(actorRef1, maxDuration)(systemAccountId, name, emailAddress)
        .asInstanceOf[CreateAccountSucceeded]
      createSystemAccountReply.accountId shouldBe systemAccountId

      val destroySystemAccountReply =
        destroyAccount(actorRef1, maxDuration)(systemAccountId).asInstanceOf[DestroyAccountSucceeded]
      destroySystemAccountReply.accountId shouldBe systemAccountId
    }
  }
}
