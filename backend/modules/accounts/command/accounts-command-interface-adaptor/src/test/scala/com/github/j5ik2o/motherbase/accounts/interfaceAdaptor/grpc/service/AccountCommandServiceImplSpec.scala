package com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.grpc.service

import java.util.UUID

import akka.actor.typed.{ ActorRef, Behavior }
import akka.util.Timeout
import com.github.j5ik2o.motherbase.accounts.commandProcessor.{
  CreateAccountCommandProcessor,
  CreateAccountCommandProcessorImpl
}
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.aggregate.{
  AccountAggregate,
  AccountAggregates,
  AccountProtocol
}
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.http.responder.{
  CreateAccountGRPCResponder,
  CreateAccountGRPCResponderImpl
}
import com.github.j5ik2o.motherbase.interfaceAdaptor.actor.ActorSpec
import com.github.j5ik2o.motherbase.interfaceAdaptor.grpc.proto.{ AccountCommandService, CreateAccountRequest }
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
    val accountCommandProcessor: CreateAccountCommandProcessor =
      new CreateAccountCommandProcessorImpl(accountRef, to)
    val responder: CreateAccountGRPCResponder = new CreateAccountGRPCResponderImpl

    accountCommandService = new AccountCommandServiceImpl(accountCommandProcessor, responder)
  }

  "AccountCommandServiceImpl" - {
    "createAccount" in {
      val createAccountRequest =
        new CreateAccountRequest().withName("Junichi Kato").withEmailAddress("j5ik2o@gmail.com")
      val response = accountCommandService.createAccount(createAccountRequest).futureValue
      response.accountId should not be empty
    }
  }

}
