package com.github.j5ik2o.motherbase.interfaceAdaptor.actor

import akka.actor.testkit.typed.TestKitSettings
import akka.actor.testkit.typed.scaladsl.{ActorTestKit, ActorTestKitBase, LogCapturing, ScalaTestWithActorTestKit}
import akka.actor.typed.ActorRef
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.TestSuite
import org.scalatest.concurrent.Waiters

import scala.concurrent.duration._

abstract class ActorSpec(testKit: ActorTestKit)
    extends ScalaTestWithActorTestKit(testKit)
    with TestSuite
    with LogCapturing
    with Waiters {

  def this() = this(ActorTestKit(ActorTestKitBase.testNameFromCallStack()))

  def this(config: String) =
    this(
      ActorTestKit(
        ActorTestKitBase.testNameFromCallStack(),
        ConfigFactory.parseString(config)
      )
    )

  def this(config: Config) =
    this(ActorTestKit(ActorTestKitBase.testNameFromCallStack(), config))

  def this(config: Config, settings: TestKitSettings) =
    this(
      ActorTestKit(
        ActorTestKitBase.testNameFromCallStack(),
        config,
        settings
      )
    )

  def killActors(actors: ActorRef[Nothing]*)(maxDuration: FiniteDuration = timeout.duration): Unit = {
    actors.foreach { actorRef => testKit.stop(actorRef, maxDuration) }
  }

}
