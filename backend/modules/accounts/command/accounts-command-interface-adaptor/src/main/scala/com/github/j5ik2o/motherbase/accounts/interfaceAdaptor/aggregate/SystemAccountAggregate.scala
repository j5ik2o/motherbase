package com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.aggregate

import java.time.Instant

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{ ActorContext, Behaviors }
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{ Effect, EventSourcedBehavior }
import com.github.j5ik2o.motherbase.accounts.domain.system.{ SystemAccount, SystemAccountId }
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.aggregate.SystemAccountProtocol._

object SystemAccountAggregate {

  sealed trait State
  final case object EmptyState                             extends State
  final case class JustState(systemAccount: SystemAccount) extends State

  def apply(id: SystemAccountId): Behavior[SystemAccountProtocol.Command] = Behaviors.setup { ctx =>
    EventSourcedBehavior[SystemAccountProtocol.Command, SystemAccountProtocol.Event, State](
      persistenceId = PersistenceId.of(id.modelName, id.value.asString, "-"),
      emptyState = EmptyState,
      commandHandler = commandHandler(ctx, id),
      eventHandler = eventHandler(ctx, id)
    )
  }

  private def eventHandler(
      ctx: ActorContext[_],
      systemAccountId: SystemAccountId
  ): (State, SystemAccountProtocol.Event) => State = { (state, event) =>
    (state, event) match {
      case (EmptyState, SystemAccountCreated(id, name, occurredAt)) if id == systemAccountId =>
        JustState(SystemAccount(id, name, occurredAt))
      case (JustState(systemAccount), SystemAccountDestroyed(id, occurredAt)) if id == systemAccountId =>
        JustState(systemAccount.destroy(occurredAt))
    }
  }

  private def commandHandler(
      ctx: ActorContext[_],
      systemAccountId: SystemAccountId
  ): (State, SystemAccountProtocol.Command) => Effect[SystemAccountProtocol.Event, State] = { (state, command) =>
    (state, command) match {
      case (EmptyState, CreateSystemAccount(id, name, replyTo)) if id == systemAccountId =>
        if (SystemAccount.canCreate(name)) {
          val now     = Instant.now
          val builder = Effect.persist[SystemAccountProtocol.Event, State](SystemAccountCreated(id, name, now))
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
      case (JustState(state), DestroySystemAccount(id, replyTo)) if id == systemAccountId =>
        if (state.canDestroy) {
          val now     = Instant.now
          val builder = Effect.persist[SystemAccountProtocol.Event, State](SystemAccountDestroyed(id, now))
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
  }
}
