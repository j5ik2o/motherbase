package com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.aggregate

import java.time.Instant

import akka.actor.typed.scaladsl.{ ActorContext, Behaviors }
import akka.actor.typed.{ ActorRef, Behavior }
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{ Effect, EventSourcedBehavior }
import com.github.j5ik2o.motherbase.accounts.domain.accounts.AccountError.{
  CantCreateAccount,
  CantDestroyAccount,
  CantGetName
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
    EventSourcedBehavior[AccountProtocol.Command, AccountEvents.Event, State](
      persistenceId = PersistenceId.of(id.modelName, id.value.asString, "-"),
      emptyState = EmptyState,
      commandHandler = commandHandler(ctx, id),
      eventHandler = eventHandler(ctx, id)
    )
  }

  private def eventHandler(
      ctx: ActorContext[_],
      systemAccountId: AccountId
  ): (State, AccountEvents.Event) => State = { (state, event) =>
    (state, event) match {
      case (EmptyState, AccountCreated(id, name, emailAddress, occurredAt)) if id == systemAccountId =>
        JustState(Account(id, name, emailAddress, occurredAt))
      case (JustState(systemAccount), AccountDestroyed(id, occurredAt)) if id == systemAccountId =>
        JustState(systemAccount.destroy(occurredAt))
    }
  }

  private def commandHandler(
      ctx: ActorContext[_],
      systemAccountId: AccountId
  ): (State, AccountProtocol.Command) => Effect[AccountEvents.Event, State] = { (state, command) =>
    (state, command) match {
      case (EmptyState, CreateAccount(id, name, emailAddress, replyTo)) if id == systemAccountId =>
        create(id, name, emailAddress, replyTo)
      case (JustState(state), DestroyAccount(id, replyTo)) if id == systemAccountId =>
        destroy(state, id, replyTo)
      case (JustState(state), GetAccountName(id, replyTo)) if id == systemAccountId =>
        Effect.reply(replyTo)(GetAccountNameSucceeded(state.id, state.name))
      case (EmptyState, GetAccountName(id, replyTo)) if id == systemAccountId =>
        Effect.reply(replyTo)(GetAccountNameFailed(id, CantGetName))
    }
  }

  private def destroy(
      state: Account,
      id: AccountId,
      replyTo: Option[ActorRef[DestroyAccountReply]]
  ): Effect[AccountEvents.Event, State] = {
    if (state.canDestroy) {
      val now     = Instant.now
      val builder = Effect.persist[Event, State](AccountDestroyed(id, now))
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
  ): Effect[AccountEvents.Event, State] = {
    if (Account.canCreate(name, emailAddress)) {
      val now = Instant.now
      val builder =
        Effect.persist[Event, State](AccountCreated(id, name, emailAddress, now))
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
