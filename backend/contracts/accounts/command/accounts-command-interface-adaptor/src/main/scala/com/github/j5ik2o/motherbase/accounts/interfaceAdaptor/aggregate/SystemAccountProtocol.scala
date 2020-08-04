package com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.aggregate

import java.time.Instant

import akka.actor.typed.ActorRef
import com.github.j5ik2o.motherbase.accounts.domain.system.{ SystemAccountId, SystemAccountName }

object SystemAccountProtocol {

  sealed trait Command

  trait CommandReply

  // --- Create

  final case class CreateSystemAccount(
      systemAccountId: SystemAccountId,
      name: SystemAccountName,
      replyTo: Option[ActorRef[CreateSystemAccountReply]]
  ) extends Command

  sealed trait CreateSystemAccountReply extends CommandReply

  final case class CreateSystemAccountSucceeded(systemAccountId: SystemAccountId) extends CreateSystemAccountReply

  final case class CreateSystemAccountFailed(systemAccountId: SystemAccountId, message: String)
      extends CreateSystemAccountReply

  // --- Destroy

  final case class DestroySystemAccount(
      systemAccountId: SystemAccountId,
      replyTo: Option[ActorRef[DestroySystemAccountReply]]
  ) extends Command

  sealed trait DestroySystemAccountReply extends CommandReply

  final case class DestroySystemAccountSucceeded(systemAccountId: SystemAccountId) extends DestroySystemAccountReply

  final case class DestroySystemAccountFailed(systemAccountId: SystemAccountId, message: String)
      extends DestroySystemAccountReply

  // --- GetSystemAccountName

  final case class GetSystemAccountName(systemAccountId: SystemAccountId, replyTo: ActorRef[GetSystemAccountNameReply])
      extends Command

  sealed trait GetSystemAccountNameReply extends CommandReply

  final case class GetSystemAccountNameSucceeded(systemAccountId: SystemAccountId, name: SystemAccountName)
      extends GetSystemAccountNameReply

  final case class GetSystemAccountNameFailed(systemAccountId: SystemAccountId, message: String)
      extends GetSystemAccountNameReply

  // ---

  sealed trait Event {
    def systemAccountId: SystemAccountId
  }

  final case class SystemAccountCreated(
      systemAccountId: SystemAccountId,
      name: SystemAccountName,
      occurredAt: Instant
  ) extends Event

  final case class SystemAccountDestroyed(
      systemAccountId: SystemAccountId,
      occurredAt: Instant
  ) extends Event

}
