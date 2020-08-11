package com.github.j5ik2o.motherbase.accounts.interfaceAdaptor

import akka.actor.typed.ActorRef

package object aggregate {
  type AccountRef = ActorRef[AccountProtocol.Command]
}
