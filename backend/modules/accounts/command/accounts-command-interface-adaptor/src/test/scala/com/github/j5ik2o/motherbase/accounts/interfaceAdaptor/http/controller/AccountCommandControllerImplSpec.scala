package com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.http.controller

import java.util.UUID

import akka.actor.typed.scaladsl.adapter._
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.cluster.typed.Cluster
import akka.http.scaladsl.model.StatusCodes
import akka.util.Timeout
import com.github.j5ik2o.motherbase.accounts.commandProcessor.CreateAccountCommandProcessorImpl
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.aggregate.{
  AccountAggregate,
  AccountAggregates,
  ClusterShardingSpecSupport,
  ShardedAccountAggregates
}
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.http.json.{
  CreateAccountRequestJson,
  CreateAccountResponseJson
}
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.http.responder.CreateAccountJsonResponderImpl
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.http.routes.RouteNames
import io.circe.generic.auto._
import org.scalatest.concurrent.Eventually
import org.scalatest.freespec.AnyFreeSpec

import scala.concurrent.duration._

class AccountCommandControllerImplSpec
    extends AnyFreeSpec
    with Eventually
    with RouteSpec
    with ClusterShardingSpecSupport {

  override def cluster: Cluster = _cluster

  override def clusterSharding: ClusterSharding = _clusterSharding

  var _cluster: Cluster                           = _
  var _clusterSharding: ClusterSharding           = _
  var commandController: AccountCommandController = _

  override def testConfigSource: String =
    s"""
      |akka.actor.provider = cluster
      |akka.persistence.journal.plugin = "akka.persistence.journal.inmem"
      |akka.persistence.snapshot-store.plugin = "akka.persistence.snapshot-store.local"
      |akka.persistence.snapshot-store.local.dir = "target/snapshot-${UUID.randomUUID().toString}"
      |""".stripMargin

  override def clusterMode: Boolean = true

  override def beforeAll(): Unit = {
    super.beforeAll()
    val accountRef = if (clusterMode) {
      _cluster = Cluster(system.toTyped)
      _clusterSharding = ClusterSharding(system.toTyped)
      prepareClusterSharding()

      ShardedAccountAggregates.initClusterSharding(
        clusterSharding,
        AccountAggregates(_.value.asString)(AccountAggregate(_)),
        Some(10 seconds)
      )

      val behavior = ShardedAccountAggregates.ofProxy(clusterSharding)
      system.spawn(behavior, "accounts")
    } else {
      val behavior = AccountAggregates(_.value.asString)(AccountAggregate(_))
      system.spawn(behavior, "accounts")
    }

    implicit val to: Timeout = 10 seconds
    val processor            = new CreateAccountCommandProcessorImpl(accountRef, to)(system.toTyped)
    val responder            = new CreateAccountJsonResponderImpl
    commandController = new AccountCommandControllerImpl(processor, responder)
  }

  "AccountCommandController" - {
    "create" in {
      val name   = "ABC"
      val email  = "ABC@ABC.com"
      val entity = CreateAccountRequestJson(name, email)
      Post(RouteNames.CreateAccount, entity) ~> commandController.createAccount ~> check {
        response.status shouldEqual StatusCodes.OK
        val responseJson = responseAs[CreateAccountResponseJson]
        responseJson.isSuccessful shouldBe true
      }

    }

  }

}
