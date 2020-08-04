package com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.aggregate

import akka.actor.typed.ActorRef
import com.github.j5ik2o.motherbase.accounts.domain.system.{ SystemAccountId, SystemAccountName }
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.aggregate.SystemAccountProtocol.{
  CreateSystemAccount,
  CreateSystemAccountReply,
  DestroySystemAccount,
  DestroySystemAccountReply,
  GetSystemAccountName,
  GetSystemAccountNameReply
}
import com.github.j5ik2o.motherbase.interfaceAdaptor.actor.ActorSpec

import scala.concurrent.duration.FiniteDuration

trait SystemAccountAggregateSpecHelper { this: ActorSpec =>

  def createSystemAccount(ref: ActorRef[SystemAccountProtocol.Command], maxDuration: FiniteDuration)(
      systemAccountId: SystemAccountId,
      name: SystemAccountName
  ): CreateSystemAccountReply = {
    val replyProbe = testKit.createTestProbe[CreateSystemAccountReply]()
    ref ! CreateSystemAccount(systemAccountId, name, Some(replyProbe.ref))
    replyProbe.expectMessageType[CreateSystemAccountReply](maxDuration)
  }

  def destroySystemAccount(ref: ActorRef[SystemAccountProtocol.Command], maxDuration: FiniteDuration)(
      systemAccountId: SystemAccountId
  ): DestroySystemAccountReply = {
    val replyProbe = testKit.createTestProbe[DestroySystemAccountReply]()
    ref ! DestroySystemAccount(systemAccountId, Some(replyProbe.ref))
    replyProbe.expectMessageType[DestroySystemAccountReply](maxDuration)
  }

  def getSystemAccountName(ref: ActorRef[SystemAccountProtocol.Command], maxDuration: FiniteDuration)(
      systemAccountId: SystemAccountId
  ): GetSystemAccountNameReply = {
    val replyProbe = testKit.createTestProbe[GetSystemAccountNameReply]()
    ref ! GetSystemAccountName(systemAccountId, replyProbe.ref)
    replyProbe.expectMessageType[GetSystemAccountNameReply](maxDuration)

  }

}
