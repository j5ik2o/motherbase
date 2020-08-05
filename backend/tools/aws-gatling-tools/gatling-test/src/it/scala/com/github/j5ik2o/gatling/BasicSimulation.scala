package com.github.j5ik2o.gatling

import com.github.j5ik2o.motherbase.interfaceAdaptor.grpc.proto.command.{CreateSystemAccountRequest, SystemAccountCommandServiceGrpc}
import com.github.j5ik2o.motherbase.interfaceAdaptor.grpc.proto.query.{AccountQueryServiceGrpc, GetSystemAccountRequest, GetSystemAccountsRequest}
import com.github.phisgr.gatling.grpc.Predef._
import com.github.phisgr.gatling.pb._
import com.typesafe.config.ConfigFactory
import de.huxhorn.sulky.ulid.ULID
import io.gatling.core.Predef._
import io.gatling.core.session.Expression
import io.grpc.Status

import scala.concurrent.duration._
import scala.util.Random

class BasicSimulation extends Simulation {

  private val config = ConfigFactory.load()

  private val grpcWriteHost =
    config.getString("motherbase.gatling.grpc.write.host")

  private val grpcWritePort =
    config.getInt("motherbase.gatling.grpc.write.port")

  private val grpcReadHost =
    config.getString("motherbase.gatling.grpc.read.host")

  private val grpcReadPort =
    config.getInt("motherbase.gatling.grpc.read.port")

  private val enableRead = config.getBoolean("motherbase.gatling.enableRead")

  private val pauseDuration =
    config.getDuration("motherbase.gatling.pause-duration").toMillis.millis

  private val readPauseDuration =
    config.getDuration("motherbase.gatling.read-pause-duration").toMillis.millis

  private val numOfUser = config.getInt("motherbase.gatling.users")

  private val rampDuration =
    config.getDuration("motherbase.gatling.ramp-duration").toMillis.millis

  private val holdDuration =
    config.getDuration("motherbase.gatling.hold-duration").toMillis.millis
  private val entireDuration = rampDuration + holdDuration

  val grpcConf = grpc(managedChannelBuilder(name = grpcWriteHost, port = grpcWritePort).usePlaintext())
    .warmUpCall(AccountQueryServiceGrpc.METHOD_GET_ACCOUNTS, GetSystemAccountsRequest.defaultInstance)

  val createAccountRequest: Expression[CreateSystemAccountRequest] =
    CreateSystemAccountRequest().updateExpr(
      { _.organizationId :~ $("orgId") }, { _.name :~ $("name") }, { _.email :~ $("email") }
    )

  val getAccountRequest = GetSystemAccountRequest().updateExpr { _.accountId :~ $("accountId") }

  val getAccount = grpc("get-account")
    .rpc(AccountQueryServiceGrpc.METHOD_GET_ACCOUNT)
    .payload(getAccountRequest)
    .check(statusCode is Status.Code.OK)

  val createAccount = grpc("create-account")
    .rpc(SystemAccountCommandServiceGrpc.METHOD_CREATE_ACCOUNT)
    .payload(createAccountRequest)
    .check(statusCode is Status.Code.OK)
    .extract(_.accountId.some)(_ saveAs "accountId")

  val ulid  = new ULID()
  def orgId = ulid.nextValue().toString
  def name  = "test-" + ulid.nextValue().toString
  def email = Random.alphanumeric.take(20).mkString + "@test.com"

  val feeder = Iterator.continually(
    Map(
      "orgId" -> orgId,
      "name"  -> name,
      "email" -> email
    )
  )

  val scn = scenario(getClass.getName)
    .forever {
      pause(pauseDuration)
      val result = feed(feeder)
        .exec(createAccount)
      if (enableRead)
        result
          .pause(readPauseDuration)
          .exec(getAccount)
      else
        result
    }

  setUp(scn.inject(rampUsers(numOfUser).during(rampDuration)))
    .protocols(grpcConf)
    .maxDuration(entireDuration)

}
