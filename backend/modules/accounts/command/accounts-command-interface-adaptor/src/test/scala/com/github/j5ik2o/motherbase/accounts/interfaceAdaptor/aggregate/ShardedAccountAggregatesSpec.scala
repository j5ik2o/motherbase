package com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.aggregate

import java.util.UUID

import akka.actor.typed.Behavior
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.cluster.typed.Cluster
import com.github.j5ik2o.motherbase.accounts.domain.accounts.AccountId
import com.github.j5ik2o.motherbase.interfaceAdaptor.actor.ActorSpec
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterAll

import scala.concurrent.duration._

class ShardedAccountAggregatesSpec
    extends ActorSpec(
      ConfigFactory
        .parseString(s"""
                    |akka.actor.provider = cluster
                    |akka.persistence.journal.plugin = "akka.persistence.journal.inmem"
                    |akka.persistence.snapshot-store.plugin = "akka.persistence.snapshot-store.local"
                    |akka.persistence.snapshot-store.local.dir = "target/snapshot-${UUID.randomUUID().toString}"
                    |""".stripMargin).withFallback(ConfigFactory.load())
    )
    with ClusterShardingSpecSupport
    with BeforeAndAfterAll
    with AccountAggregateSpecScenario {

  val cluster: Cluster                 = Cluster(system)
  val clusterSharding: ClusterSharding = ClusterSharding(system)

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    prepareClusterSharding()
    ShardedAccountAggregates.initClusterSharding(
      clusterSharding,
      AccountAggregates(_.value.asString)(AccountAggregate(_)),
      Some(10 seconds)
    )
  }

  override def behavior(systemAccountId: AccountId): Behavior[AccountProtocol.Command] =
    ShardedAccountAggregates.ofProxy(clusterSharding)

}
