package com.github.j5ik2o.motherbase.accounts.interfaceAdaptor

import akka.actor.typed.{ ActorRef, Behavior }
import akka.actor.typed.scaladsl.ActorContext

package object aggregate {
  type AccountBehavior = Behavior[AccountProtocol.Command]
  type AccountRef      = ActorRef[AccountProtocol.Command]
  type AccountRefF     = (ActorContext[AccountProtocol.Command]) => AccountRef
}
