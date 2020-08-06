package com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.aggregate

import java.time.Instant

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}
import com.github.j5ik2o.motherbase.accounts.domain.event.SystemAccountEvents
import com.github.j5ik2o.motherbase.accounts.domain.event.SystemAccountEvents._
import com.github.j5ik2o.motherbase.accounts.domain.system.{EmailAddress, SystemAccount, SystemAccountId, SystemAccountName}
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.aggregate.SystemAccountProtocol._

object SystemAccountAggregate {

  sealed trait State
  final case object EmptyState                             extends State
  final case class JustState(systemAccount: SystemAccount) extends State

  def apply(id: SystemAccountId): Behavior[SystemAccountProtocol.Command] = Behaviors.setup { ctx =>
    EventSourcedBehavior[SystemAccountProtocol.Command, SystemAccountEvents.Event, State](
      persistenceId = PersistenceId.of(id.modelName, id.value.asString, "-"),
      emptyState = EmptyState,
      commandHandler = commandHandler(ctx, id),
      eventHandler = eventHandler(ctx, id)
    )
  }

  private def eventHandler(
      ctx: ActorContext[_],
      systemAccountId: SystemAccountId
  ): (State, SystemAccountEvents.Event) => State = { (state, event) =>
    (state, event) match {
      case (EmptyState, SystemAccountCreated(id, name, emailAddress, occurredAt)) if id == systemAccountId =>
        JustState(SystemAccount(id, name, emailAddress, occurredAt))
      case (JustState(systemAccount), SystemAccountDestroyed(id, occurredAt)) if id == systemAccountId =>
        JustState(systemAccount.destroy(occurredAt))
    }
  }

  private def commandHandler(
      ctx: ActorContext[_],
      systemAccountId: SystemAccountId
  ): (State, SystemAccountProtocol.Command) => Effect[SystemAccountEvents.Event, State] = { (state, command) =>
    (state, command) match {
      case (EmptyState, CreateSystemAccount(id, name, emailAddress, replyTo)) if id == systemAccountId =>
        create(id, name, emailAddress, replyTo)
      case (JustState(state), DestroySystemAccount(id, replyTo)) if id == systemAccountId =>
        destroy(state, id, replyTo)
      case (JustState(state), GetSystemAccountName(id, replyTo)) if id == systemAccountId =>
        Effect.reply(replyTo)(GetSystemAccountNameSucceeded(state.id, state.name))
      case (EmptyState, GetSystemAccountName(id, replyTo)) if id == systemAccountId =>
        Effect.reply(replyTo)(GetSystemAccountNameFailed(id, "Can't get name"))
    }
  }

  private def destroy(
      state: SystemAccount,
      id: SystemAccountId,
      replyTo: Option[ActorRef[DestroySystemAccountReply]]
  ): Effect[SystemAccountEvents.Event, State] = {
    if (state.canDestroy) {
      val now     = Instant.now
      val builder = Effect.persist[Event, State](SystemAccountDestroyed(id, now))
      replyTo match {
        case None    => builder
        case Some(r) => builder.thenReply(r)(_ => DestroySystemAccountSucceeded(id))
      }
    } else {
      replyTo match {
        case None    => Effect.none
        case Some(r) => Effect.reply(r)(DestroySystemAccountFailed(id, "Can't destroy SystemAccount"))
      }
    }
  }

  private def create(
      id: SystemAccountId,
      name: SystemAccountName,
      emailAddress: EmailAddress,
      replyTo: Option[ActorRef[CreateSystemAccountReply]]
  ): Effect[SystemAccountEvents.Event, State] = {
    if (SystemAccount.canCreate(name, emailAddress)) {
      val now = Instant.now
      val builder =
        Effect.persist[Event, State](SystemAccountCreated(id, name, emailAddress, now))
      replyTo match {
        case None =>
          builder
        case Some(r) =>
          builder.thenReply(r)(_ => CreateSystemAccountSucceeded(id))
      }
    } else {
      replyTo match {
        case None    => Effect.none
        case Some(r) => Effect.reply(r)(CreateSystemAccountFailed(id, "Can't create SystemAccount"))
      }
    }
  }
}
