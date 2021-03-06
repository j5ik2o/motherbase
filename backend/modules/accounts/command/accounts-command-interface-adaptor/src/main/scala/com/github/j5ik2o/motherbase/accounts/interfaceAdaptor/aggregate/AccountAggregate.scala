package com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.aggregate

import java.time.Instant

import akka.actor.typed.scaladsl.{ ActorContext, Behaviors }
import akka.actor.typed.{ ActorRef, Behavior }
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{ Effect, EventSourcedBehavior }
import com.github.j5ik2o.motherbase.accounts.domain.accounts.AccountError.{
  CantCreateAccount,
  CantDestroyAccount,
  CantGetName,
  CantRenameAccount
}
import com.github.j5ik2o.motherbase.accounts.domain.accounts.AccountEvents._
import com.github.j5ik2o.motherbase.accounts.domain.accounts.{
  Account,
  AccountEvents,
  AccountId,
  AccountName,
  EmailAddress
}
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.aggregate.AccountProtocol._

object AccountAggregate {

  sealed trait State
  final case object EmptyState                       extends State
  final case class JustState(systemAccount: Account) extends State

  def apply(id: AccountId): Behavior[AccountProtocol.Command] = Behaviors.setup { ctx =>
    EventSourcedBehavior[AccountProtocol.Command, AccountEvents.AccountEvent, State](
      persistenceId = PersistenceId.of(id.modelName, id.value.asString, "-"),
      emptyState = EmptyState,
      commandHandler = commandHandler(ctx, id),
      eventHandler = eventHandler(ctx, id)
    )
  }

  private def eventHandler(
      ctx: ActorContext[_],
      accountId: AccountId
  ): (State, AccountEvents.AccountEvent) => State = { (state, event) =>
    (state, event) match {
      case (EmptyState, AccountCreated(id, name, emailAddress, occurredAt)) if id == accountId =>
        JustState(Account(id, name, emailAddress, occurredAt))
      case (JustState(state), AccountRenamed(id, name, occurredAt)) if id == accountId =>
        JustState(state.rename(name, occurredAt))
      case (JustState(state), AccountDestroyed(id, occurredAt)) if id == accountId =>
        JustState(state.destroy(occurredAt))
    }
  }

  private def commandHandler(
      ctx: ActorContext[_],
      accountId: AccountId
  ): (State, AccountProtocol.Command) => Effect[AccountEvents.AccountEvent, State] = { (state, command) =>
    (state, command) match {
      case (EmptyState, CreateAccount(id, name, emailAddress, replyTo)) if id == accountId =>
        create(id, name, emailAddress, replyTo)
      case (JustState(state), RenameAccount(id, name, replyTo)) if id == accountId =>
        rename(state, id, name, replyTo)
      case (JustState(state), DestroyAccount(id, replyTo)) if id == accountId =>
        destroy(state, id, replyTo)
      case (JustState(state), GetAccountName(id, replyTo)) if id == accountId =>
        Effect.reply(replyTo)(GetAccountNameSucceeded(state.id, state.name))
      case (EmptyState, GetAccountName(id, replyTo)) if id == accountId =>
        Effect.reply(replyTo)(GetAccountNameFailed(id, CantGetName))
    }
  }

  private def rename(
      state: Account,
      id: AccountId,
      name: AccountName,
      replyTo: Option[ActorRef[RenameAccountReply]]
  ): Effect[AccountEvents.AccountEvent, State] = {
    if (state.canRename(name)) {
      val now     = Instant.now
      val builder = Effect.persist[AccountEvent, State](AccountRenamed(id, name, now))
      replyTo match {
        case None    => builder
        case Some(r) => builder.thenReply(r)(_ => RenameAccountSucceeded(id))
      }
    } else {
      replyTo match {
        case None    => Effect.none
        case Some(r) => Effect.reply(r)(RenameAccountFailed(id, CantRenameAccount))
      }
    }
  }

  private def destroy(
      state: Account,
      id: AccountId,
      replyTo: Option[ActorRef[DestroyAccountReply]]
  ): Effect[AccountEvents.AccountEvent, State] = {
    if (state.canDestroy) {
      val now     = Instant.now
      val builder = Effect.persist[AccountEvent, State](AccountDestroyed(id, now))
      replyTo match {
        case None    => builder
        case Some(r) => builder.thenReply(r)(_ => DestroyAccountSucceeded(id))
      }
    } else {
      replyTo match {
        case None    => Effect.none
        case Some(r) => Effect.reply(r)(DestroyAccountFailed(id, CantDestroyAccount))
      }
    }
  }

  private def create(
      id: AccountId,
      name: AccountName,
      emailAddress: EmailAddress,
      replyTo: Option[ActorRef[CreateAccountReply]]
  ): Effect[AccountEvents.AccountEvent, State] = {
    if (Account.canCreate(name, emailAddress)) {
      val now = Instant.now
      val builder =
        Effect.persist[AccountEvent, State](AccountCreated(id, name, emailAddress, now))
      replyTo match {
        case None =>
          builder
        case Some(r) =>
          builder.thenReply(r)(_ => CreateAccountSucceeded(id))
      }
    } else {
      replyTo match {
        case None    => Effect.none
        case Some(r) => Effect.reply(r)(CreateAccountFailed(id, CantCreateAccount))
      }
    }
  }
}
