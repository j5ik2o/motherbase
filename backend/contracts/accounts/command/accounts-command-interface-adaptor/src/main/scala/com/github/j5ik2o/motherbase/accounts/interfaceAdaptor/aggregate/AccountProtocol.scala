package com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.aggregate

import akka.actor.typed.ActorRef
import com.github.j5ik2o.motherbase.accounts.domain.accounts.{ AccountError, AccountId, AccountName, EmailAddress }

object AccountProtocol {

  sealed trait Command {
    def accountId: AccountId
  }

  sealed trait CommandReply {
    def accountId: AccountId
  }

  final case object Idle extends Command {
    override def accountId: AccountId = throw new UnsupportedOperationException
  }

  final case object Stop extends Command {
    override def accountId: AccountId = throw new UnsupportedOperationException
  }

  // --- Create

  final case class CreateAccount(
      accountId: AccountId,
      name: AccountName,
      emailAddress: EmailAddress,
      replyTo: Option[ActorRef[CreateAccountReply]]
  ) extends Command

  sealed trait CreateAccountReply extends CommandReply

  final case class CreateAccountSucceeded(accountId: AccountId) extends CreateAccountReply

  final case class CreateAccountFailed(accountId: AccountId, error: AccountError) extends CreateAccountReply

  // --- Rename

  final case class RenameAccount(
      accountId: AccountId,
      name: AccountName,
      replyTo: Option[ActorRef[RenameAccountReply]]
  ) extends Command

  sealed trait RenameAccountReply extends CommandReply

  final case class RenameAccountSucceeded(accountId: AccountId) extends RenameAccountReply

  final case class RenameAccountFailed(accountId: AccountId, error: AccountError) extends RenameAccountReply

  // --- Destroy

  final case class DestroyAccount(
      accountId: AccountId,
      replyTo: Option[ActorRef[DestroyAccountReply]]
  ) extends Command

  sealed trait DestroyAccountReply extends CommandReply

  final case class DestroyAccountSucceeded(accountId: AccountId) extends DestroyAccountReply

  final case class DestroyAccountFailed(accountId: AccountId, error: AccountError) extends DestroyAccountReply

  // --- GetAccountName

  final case class GetAccountName(accountId: AccountId, replyTo: ActorRef[GetAccountNameReply]) extends Command

  sealed trait GetAccountNameReply extends CommandReply

  final case class GetAccountNameSucceeded(accountId: AccountId, name: AccountName) extends GetAccountNameReply

  final case class GetAccountNameFailed(accountId: AccountId, error: AccountError) extends GetAccountNameReply

}
