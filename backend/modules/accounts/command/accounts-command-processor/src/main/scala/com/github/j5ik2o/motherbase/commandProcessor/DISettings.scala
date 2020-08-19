package com.github.j5ik2o.motherbase.commandProcessor

import akka.actor.typed.ActorSystem
import akka.stream.Materializer
import akka.util.Timeout
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.aggregate.AccountRef
import wvlet.airframe._

import scala.concurrent.duration._

object DISettings {

  def design: Design = {
    newDesign.bind[CreateAccountCommandProcessor].toProvider[AccountRef, ActorSystem[Nothing]] {
      case (accountRef, system) =>
        implicit val to = Timeout(10 seconds)
        new CreateAccountCommandProcessorImpl(accountRef, to)(system)
    }
  }

}
