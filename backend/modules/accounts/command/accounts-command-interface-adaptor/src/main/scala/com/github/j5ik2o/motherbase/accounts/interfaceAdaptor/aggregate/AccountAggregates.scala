package com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.aggregate

import akka.actor.typed.{ ActorRef, Behavior }
import akka.actor.typed.scaladsl.Behaviors
import com.github.j5ik2o.motherbase.accounts.domain.accounts.AccountId

object AccountAggregates {
  val name = "accounts"

  def apply(nameF: AccountId => String)(
      childBehaviorF: AccountId => Behavior[AccountProtocol.Command]
  ): Behavior[AccountProtocol.Command] = {
    Behaviors.setup { ctx =>
      def getOrCreateRef(accountId: AccountId): ActorRef[AccountProtocol.Command] = {
        ctx.child(nameF(accountId)) match {
          case None =>
            ctx.log.debug(s"spawn: child = $accountId")
            ctx.spawn(childBehaviorF(accountId), name = nameF(accountId))
          case Some(ref) =>
            ref.asInstanceOf[ActorRef[AccountProtocol.Command]]
        }
      }
      Behaviors.receiveMessage[AccountProtocol.Command] { msg =>
        getOrCreateRef(msg.accountId) ! msg
        Behaviors.same
      }
    }
  }
}
