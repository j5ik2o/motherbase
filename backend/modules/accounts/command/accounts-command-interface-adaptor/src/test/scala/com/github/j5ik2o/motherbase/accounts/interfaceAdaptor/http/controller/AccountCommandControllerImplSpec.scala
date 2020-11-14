package com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.http.controller

import java.util.UUID

import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.adapter._
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.cluster.typed.Cluster
import akka.http.scaladsl.model.StatusCodes
import akka.util.Timeout
import com.github.j5ik2o.motherbase.accounts.commandProcessor.{
  CreateAccountCommandProcessorImpl,
  RenameAccountCommandProcessorImpl
}
import com.github.j5ik2o.motherbase.accounts.domain.accounts.{ AccountId, AccountName }
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.aggregate.AccountProtocol.{
  GetAccountName,
  GetAccountNameReply,
  GetAccountNameSucceeded
}
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.aggregate.{
  AccountAggregate,
  AccountAggregates,
  AccountProtocol,
  ClusterShardingSpecSupport,
  ShardedAccountAggregates
}
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.http.json.{
  CreateAccountRequestJson,
  CreateAccountResponseJson,
  RenameAccountRequestJson,
  RenameAccountResponseJson
}
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.http.responder.{
  CreateAccountJsonResponderImpl,
  RenameAccountJsonResponderImpl
}
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

  var _cluster: Cluster                             = _
  var _clusterSharding: ClusterSharding             = _
  var commandController: AccountCommandController   = _
  var accountRef: ActorRef[AccountProtocol.Command] = _

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
    accountRef = if (clusterMode) {
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

    implicit val to: Timeout          = 10 seconds
    val createAccountCommandProcessor = new CreateAccountCommandProcessorImpl(accountRef, to)(system.toTyped)
    val createAccountJsonResponder    = new CreateAccountJsonResponderImpl
    val renameAccountCommandProcessor = new RenameAccountCommandProcessorImpl(accountRef, to)(system.toTyped)
    val renameAccountJsonResponder    = new RenameAccountJsonResponderImpl

    commandController = new AccountCommandControllerImpl(
      createAccountCommandProcessor,
      createAccountJsonResponder,
      renameAccountCommandProcessor,
      renameAccountJsonResponder
    )
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
        val probe     = TestProbe[GetAccountNameReply]()(system.toTyped)
        val accountId = AccountId(responseJson.account_id)
        accountRef ! GetAccountName(accountId, probe.ref)
        probe.expectMessage(GetAccountNameSucceeded(accountId, AccountName(name)))
      }
    }
    "rename" in {
      val name1                    = "ABC"
      val email                    = "ABC@ABC.com"
      val createAccountRequestJson = CreateAccountRequestJson(name1, email)
      Post(RouteNames.CreateAccount, createAccountRequestJson) ~> commandController.createAccount ~> check {
        response.status shouldEqual StatusCodes.OK
        val responseJson = responseAs[CreateAccountResponseJson]
        responseJson.isSuccessful shouldBe true
        val probe     = TestProbe[GetAccountNameReply]()(system.toTyped)
        val accountId = AccountId(responseJson.account_id)
        accountRef ! GetAccountName(accountId, probe.ref)
        probe.expectMessage(GetAccountNameSucceeded(accountId, AccountName(name1)))
        val name2                    = "DEF"
        val renameAccountRequestJson = RenameAccountRequestJson(responseJson.account_id, name2)
        Post(RouteNames.RenameAccount(responseJson.account_id), renameAccountRequestJson) ~> commandController.renameAccount ~> check {
          response.status shouldEqual StatusCodes.OK
          val responseJson = responseAs[RenameAccountResponseJson]
          responseJson.isSuccessful shouldBe true
          val probe = TestProbe[GetAccountNameReply]()(system.toTyped)
          accountRef ! GetAccountName(accountId, probe.ref)
          probe.expectMessage(GetAccountNameSucceeded(accountId, AccountName(name2)))
        }
      }
    }

  }

}
