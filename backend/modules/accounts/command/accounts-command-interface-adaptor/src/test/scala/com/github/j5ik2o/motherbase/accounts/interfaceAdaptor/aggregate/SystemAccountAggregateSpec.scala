package com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.aggregate

import java.util.UUID

import akka.actor.typed.Behavior
import com.github.j5ik2o.motherbase.accounts.domain.system.SystemAccountId
import com.github.j5ik2o.motherbase.interfaceAdaptor.actor.ActorSpec
import com.typesafe.config.ConfigFactory

class SystemAccountAggregateSpec
    extends ActorSpec(
      ConfigFactory
        .parseString(s"""
                      |akka.persistence.journal.plugin = "akka.persistence.journal.inmem"
                      |akka.persistence.snapshot-store.plugin = "akka.persistence.snapshot-store.local"
                      |akka.persistence.snapshot-store.local.dir = "target/snapshot-${UUID.randomUUID().toString}"
                      |""".stripMargin).withFallback(ConfigFactory.load())
    )
    with SystemAccountAggregateSpecHelper
    with SystemAccountAggregateSpecScenario {

  override def behavior(systemAccountId: SystemAccountId): Behavior[SystemAccountProtocol.Command] =
    SystemAccountAggregate(systemAccountId)
}
