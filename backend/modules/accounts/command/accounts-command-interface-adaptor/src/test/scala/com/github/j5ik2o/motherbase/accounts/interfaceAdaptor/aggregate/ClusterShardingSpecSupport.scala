package com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.aggregate

import akka.cluster.MemberStatus
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.cluster.typed.{ Cluster, Join }
import org.scalatest.concurrent.Eventually
import org.scalatest.matchers.should.Matchers

trait ClusterShardingSpecSupport extends Eventually with Matchers {

  def cluster: Cluster
  def clusterSharding: ClusterSharding

  def prepareClusterSharding(): Unit = {
    cluster.manager ! Join(cluster.selfMember.address)
    eventually {
      cluster.selfMember.status shouldEqual MemberStatus.Up
    }
  }

}
