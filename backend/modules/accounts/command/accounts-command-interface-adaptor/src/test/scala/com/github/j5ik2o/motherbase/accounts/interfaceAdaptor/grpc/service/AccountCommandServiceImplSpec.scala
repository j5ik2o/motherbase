package com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.grpc.service

import java.util.UUID

import akka.actor.typed.{ ActorRef, Behavior }
import akka.util.Timeout
import com.github.j5ik2o.motherbase.accounts.commandProcessor.{
  CreateAccountCommandProcessor,
  CreateAccountCommandProcessorImpl,
  RenameAccountCommandProcessor,
  RenameAccountCommandProcessorImpl
}
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.aggregate.{
  AccountAggregate,
  AccountAggregates,
  AccountProtocol
}
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.http.responder.{
  CreateAccountGRPCResponder,
  CreateAccountGRPCResponderImpl,
  RenameAccountGRPCResponder,
  RenameAccountGRPCResponderImpl
}
import com.github.j5ik2o.motherbase.interfaceAdaptor.actor.ActorSpec
import com.github.j5ik2o.motherbase.interfaceAdaptor.grpc.proto.{
  AccountCommandService,
  CreateAccountRequest,
  RenameAccountRequest
}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpecLike
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration._

class AccountCommandServiceImplSpec extends ActorSpec(s"""
   |akka.actor.provider = cluster
   |akka.persistence.journal.plugin = "akka.persistence.journal.inmem"
   |akka.persistence.snapshot-store.plugin = "akka.persistence.snapshot-store.local"
   |akka.persistence.snapshot-store.local.dir = "target/snapshot-${UUID.randomUUID().toString}"
   |""".stripMargin) with AnyFreeSpecLike with ScalaFutures with Matchers {
  var accountCommandService: AccountCommandService = _

  override def beforeAll(): Unit = {
    super.beforeAll()
    implicit val to: Timeout = 10 seconds

    val behavior: Behavior[AccountProtocol.Command]   = AccountAggregates(_.value.asString)(AccountAggregate(_))
    val accountRef: ActorRef[AccountProtocol.Command] = spawn(behavior, "accounts")

    val createAccountCommandProcessor: CreateAccountCommandProcessor =
      new CreateAccountCommandProcessorImpl(accountRef, to)
    val createAccountGRPCResponder: CreateAccountGRPCResponder = new CreateAccountGRPCResponderImpl

    val renameAccountCommandProcessor: RenameAccountCommandProcessor =
      new RenameAccountCommandProcessorImpl(accountRef, to)
    val renameAccountGRPCResponder: RenameAccountGRPCResponder = new RenameAccountGRPCResponderImpl

    accountCommandService = new AccountCommandServiceImpl(
      createAccountCommandProcessor,
      createAccountGRPCResponder,
      renameAccountCommandProcessor,
      renameAccountGRPCResponder
    )
  }

  "AccountCommandServiceImpl" - {
    "createAccount" in {
      val createAccountRequest =
        new CreateAccountRequest().withName("Junichi Kato").withEmailAddress("j5ik2o@gmail.com")
      val response = accountCommandService.createAccount(createAccountRequest).futureValue
      response.accountId should not be empty
      response.errors shouldBe empty
    }
    "renameAccount" in {
      val createAccountRequest =
        new CreateAccountRequest().withName("AccountName-1").withEmailAddress("j5ik2o@gmail.com")
      val response1 = accountCommandService.createAccount(createAccountRequest).futureValue
      response1.accountId should not be empty
      response1.errors shouldBe empty
      val renameAccountRequest = new RenameAccountRequest().withAccountId(response1.accountId).withName("AccountName-2")
      val response2            = accountCommandService.renameAccount(renameAccountRequest).futureValue
      response2.accountId should not be empty
      response2.errors shouldBe empty
    }
//    "destroyAccount" in {
//      val destroyAccountRequest =
//        new DestroyAccountRequest().withName("Junichi Kato").withEmailAddress("j5ik2o@gmail.com")
//      val response = accountCommandService.createAccount(createAccountRequest).futureValue
//      response.accountId should not be empty
//      response.errors shouldBe empty
//    }

  }

}
