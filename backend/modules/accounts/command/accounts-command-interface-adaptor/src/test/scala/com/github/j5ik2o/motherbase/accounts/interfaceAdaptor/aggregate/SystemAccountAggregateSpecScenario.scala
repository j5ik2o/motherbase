package com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.aggregate

import akka.actor.typed.{ ActorRef, Behavior }
import com.github.j5ik2o.motherbase.accounts.domain.system.{ SystemAccountId, SystemAccountName }
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.aggregate.SystemAccountProtocol.GetSystemAccountNameSucceeded
import com.github.j5ik2o.motherbase.interfaceAdaptor.actor.ActorSpec

import scala.concurrent.duration._

trait SystemAccountAggregateSpecScenario {
  this: ActorSpec with SystemAccountAggregateSpecHelper =>
  val maxDuration: FiniteDuration = (10 * sys.env.getOrElse("SBT_TEST_TIME_FACTOR", "1").toInt) seconds

  def behavior(systemAccountId: SystemAccountId): Behavior[SystemAccountProtocol.Command]

  def actorRef(systemAccountId: SystemAccountId): ActorRef[SystemAccountProtocol.Command] =
    spawn(behavior(systemAccountId))

  "SystemAccountPersistentAggregate" - {
    "create with killActor" in {
      val systemAccountId = SystemAccountId()
      val name            = SystemAccountName("test")
      val actorRef1       = actorRef(systemAccountId)
      createSystemAccount(actorRef1, maxDuration)(systemAccountId, name)

      killActors(actorRef1)((10 * sys.env.getOrElse("SBT_TEST_TIME_FACTOR", "1").toInt) seconds)

      val actorRef2 = actorRef(systemAccountId)

      eventually {
        val getSystemAccountNameReply2 =
          getSystemAccountName(actorRef2, maxDuration)(systemAccountId).asInstanceOf[GetSystemAccountNameSucceeded]
        getSystemAccountNameReply2.name shouldBe name
      }
    }
  }
}
