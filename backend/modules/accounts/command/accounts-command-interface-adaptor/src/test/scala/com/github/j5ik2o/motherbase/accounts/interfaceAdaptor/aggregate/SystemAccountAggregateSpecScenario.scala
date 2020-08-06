package com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.aggregate

import akka.actor.typed.{ActorRef, Behavior}
import com.github.j5ik2o.motherbase.accounts.domain.system.{EmailAddress, SystemAccountId, SystemAccountName}
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.aggregate.SystemAccountProtocol.{CreateSystemAccountSucceeded, DestroySystemAccountSucceeded, GetSystemAccountNameSucceeded}
import com.github.j5ik2o.motherbase.interfaceAdaptor.actor.ActorSpec
import org.scalatest.freespec.AnyFreeSpecLike

trait SystemAccountAggregateSpecScenario
    extends AnyFreeSpecLike
    with AggregateSpecScenarioBase
    with SystemAccountAggregateSpecHelper {
  this: ActorSpec =>

  def behavior(systemAccountId: SystemAccountId): Behavior[SystemAccountProtocol.Command]

  def actorRef(systemAccountId: SystemAccountId): ActorRef[SystemAccountProtocol.Command] =
    spawn(behavior(systemAccountId))

  "SystemAccountAggregate" - {
    "create with killActor" in {
      val systemAccountId = SystemAccountId()
      val name            = SystemAccountName("test")
      val emailAddress    = EmailAddress("test@test.com")
      val actorRef1       = actorRef(systemAccountId)

      val createSystemAccountReply = createSystemAccount(actorRef1, maxDuration)(systemAccountId, name, emailAddress)
        .asInstanceOf[CreateSystemAccountSucceeded]
      createSystemAccountReply.systemAccountId shouldBe systemAccountId

      killActors(actorRef1)(maxDuration)

      val actorRef2 = actorRef(systemAccountId)

      val getSystemAccountNameReply =
        getSystemAccountName(actorRef2, maxDuration)(systemAccountId).asInstanceOf[GetSystemAccountNameSucceeded]
      getSystemAccountNameReply.name shouldBe name
    }
    "destroy" in {
      val systemAccountId = SystemAccountId()
      val name            = SystemAccountName("test")
      val emailAddress    = EmailAddress("test@test.com")
      val actorRef1       = actorRef(systemAccountId)

      val createSystemAccountReply = createSystemAccount(actorRef1, maxDuration)(systemAccountId, name, emailAddress)
        .asInstanceOf[CreateSystemAccountSucceeded]
      createSystemAccountReply.systemAccountId shouldBe systemAccountId

      val destroySystemAccountReply =
        destroySystemAccount(actorRef1, maxDuration)(systemAccountId).asInstanceOf[DestroySystemAccountSucceeded]
      destroySystemAccountReply.systemAccountId shouldBe systemAccountId
    }
  }
}
