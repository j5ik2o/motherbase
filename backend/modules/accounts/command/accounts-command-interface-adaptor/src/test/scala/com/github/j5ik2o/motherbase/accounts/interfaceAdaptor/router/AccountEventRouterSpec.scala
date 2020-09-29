package com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.router

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.AskPattern._
import com.amazonaws.services.dynamodbv2.streamsadapter.model.RecordAdapter
import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.{ IRecordProcessor, IRecordProcessorFactory }
import com.amazonaws.services.kinesis.clientlibrary.types.{ InitializationInput, ProcessRecordsInput, ShutdownInput }
import com.dimafeng.testcontainers.{ ForAllTestContainer, MultipleContainers }
import com.github.j5ik2o.motherbase.accounts.domain.accounts.{ AccountId, AccountName, EmailAddress }
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.aggregate.AccountProtocol.CreateAccountSucceeded
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.aggregate.{
  AccountAggregate,
  AccountAggregateSpecHelper,
  AggregateSpecScenarioBase,
  MotherBaseBucketNameResolver
}
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.router.AccountEventRouter.{
  Start,
  StopWithReply,
  Stopped
}
import com.github.j5ik2o.motherbase.infrastructure.ulid.ULID
import com.github.j5ik2o.motherbase.interfaceAdaptor.actor._
import com.github.j5ik2o.motherbase.interfaceAdaptor.aws.{
  AmazonCloudWatchUtil,
  AmazonDynamoDBStreamsUtil,
  AmazonDynamoDBUtil,
  CredentialsProviderUtil
}
import com.github.j5ik2o.motherbase.interfaceAdaptor.util.RandomPortUtil
import com.typesafe.config.ConfigFactory
import org.scalatest.freespec.AnyFreeSpecLike

import scala.jdk.CollectionConverters._

object AccountEventRouterSpec {

  val defaultAwsAccessKeyId = "x"
  val defaultAwsSecretKey   = "x"

  val dynamoDbPort: Int = RandomPortUtil.temporaryServerPort()

  val minioAccessKeyId     = "AKIAIOSFODNN7EXAMPLE"
  val minioSecretAccessKey = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY"
  val minioPort            = RandomPortUtil.temporaryServerPort()

  val cloudwatchPort = RandomPortUtil.temporaryServerPort()
}

class AccountEventRouterSpec
    extends ActorSpec(ConfigFactory.parseString(s"""
                                                 |akka.persistence.journal.plugin = "j5ik2o.dynamo-db-journal"
                                                 |akka.persistence.snapshot-store.plugin = "j5ik2o.s3-snapshot-store"
                                                 |j5ik2o {
                                                 |  dynamo-db-journal {
                                                 |    class = "com.github.j5ik2o.akka.persistence.dynamodb.journal.DynamoDBJournal"
                                                 |    plugin-dispatcher = "akka.actor.default-dispatcher"
                                                 |    dynamo-db-client {
                                                 |      access-key-id = "x"
                                                 |      secret-access-key = "x"
                                                 |      endpoint = "http://127.0.0.1:${AccountEventRouterSpec.dynamoDbPort}/"
                                                 |    }
                                                 |  }
                                                 |  s3-snapshot-store {
                                                 |    motherbase.bucket-name = "motherbase-test"
                                                 |    class = "com.github.j5ik2o.akka.persistence.s3.snapshot.S3SnapshotStore"
                                                 |    bucket-name-resolver-class-name = "com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.aggregate.MotherBaseBucketNameResolver"
                                                 |    key-converter-class-name = "com.github.j5ik2o.akka.persistence.s3.resolver.KeyConverter$$PersistenceId"
                                                 |    path-prefix-resolver-class-name = "com.github.j5ik2o.akka.persistence.s3.resolver.PathPrefixResolver$$PersistenceId"
                                                 |    extension-name = "snapshot"
                                                 |    max-load-attempts = 3
                                                 |    s3-client {
                                                 |      access-key-id = "${AccountEventRouterSpec.minioAccessKeyId}"
                                                 |      secret-access-key = "${AccountEventRouterSpec.minioSecretAccessKey}"
                                                 |      endpoint = "http://127.0.0.1:${AccountEventRouterSpec.minioPort}"
                                                 |      s3-options {
                                                 |        path-style-access-enabled = true
                                                 |      }
                                                 |    }
                                                 |  }
                                                 |}
                                                 |event-router.accounts {
                                                 |  access-key-id = "${AccountEventRouterSpec.defaultAwsAccessKeyId}"
                                                 |  secret-access-key = "${AccountEventRouterSpec.defaultAwsSecretKey}"
                                                 |  dynamodb-client {
                                                 |    access-key-id = "${AccountEventRouterSpec.defaultAwsAccessKeyId}"
                                                 |    secret-access-key = "${AccountEventRouterSpec.defaultAwsSecretKey}"
                                                 |    endpoint = "http://127.0.0.1:${AccountEventRouterSpec.dynamoDbPort}/"
                                                 |  }
                                                 |  dynamodb-stream-client {
                                                 |    access-key-id = "${AccountEventRouterSpec.defaultAwsAccessKeyId}"
                                                 |    secret-access-key = "${AccountEventRouterSpec.defaultAwsSecretKey}"
                                                 |    endpoint = "http://127.0.0.1:${AccountEventRouterSpec.dynamoDbPort}/"
                                                 |  }
                                                 |  cloudwatch-client {
                                                 |    access-key-id = "${AccountEventRouterSpec.defaultAwsAccessKeyId}"
                                                 |    secret-access-key = "${AccountEventRouterSpec.defaultAwsSecretKey}"
                                                 |    endpoint = "http://127.0.0.1:${AccountEventRouterSpec.cloudwatchPort}/"
                                                 |  }
                                                 |  application-name = "test"
                                                 |  initial-position-in-stream = "TRIM_HORIZON"
                                                 |}
                                                 |""".stripMargin).withFallback(ConfigFactory.load()))
    with AnyFreeSpecLike
    with ForAllTestContainer
    with DynamoDbSpecSupport
    with JournalTableSpecSupport
    with S3SpecSupport
    with LocalStackSpecSupport
    with AggregateSpecScenarioBase
    with AccountAggregateSpecHelper {
  override protected lazy val dynamoDBPort: Int       = AccountEventRouterSpec.dynamoDbPort
  override protected def minioAccessKeyId: String     = AccountEventRouterSpec.minioAccessKeyId
  override protected def minioSecretAccessKey: String = AccountEventRouterSpec.minioSecretAccessKey
  override protected def minioPort: Int               = AccountEventRouterSpec.minioPort

  override protected def localStackPort: Int = AccountEventRouterSpec.cloudwatchPort

  override lazy val container =
    MultipleContainers(dynamoDbLocalContainer, minioContainer, localStackContainer)

  override protected def s3BucketName(system: ActorSystem[_]): String =
    new MotherBaseBucketNameResolver(system.settings.config.getConfig("j5ik2o.s3-snapshot-store")).resolve(null)

  override def afterStart(): Unit = {
    implicit val ec = system.executionContext
    createS3Bucket()
    createJournalTable()
  }

  override def beforeStop() = {
    deleteJournalTable()
  }

  "AccountEventRouterSpec" - {
    "routing" in {

      val rootConfig                           = system.settings.config
      val id                                   = ULID()
      val config                               = rootConfig.getConfig("event-router.accounts")
      val accessKeyId                          = config.getString("access-key-id")
      val secretAccessKey                      = config.getString("secret-access-key")
      val accountEventRouterClientConfig       = config.getConfig("dynamodb-client")
      val accountEventRouterStreamClientConfig = config.getConfig("dynamodb-stream-client")
      val accountEventRouterCloudWatchConfig   = config.getConfig("cloudwatch-client")
      val amazonDynamoDB                       = AmazonDynamoDBUtil.createFromConfig(accountEventRouterClientConfig)
      val amazonDynamoDBStreams                = AmazonDynamoDBStreamsUtil.createFromConfig(accountEventRouterStreamClientConfig)
      val amazonCloudwatch                     = AmazonCloudWatchUtil.createFromConfig(accountEventRouterCloudWatchConfig)

      val credentialsProvider =
        CredentialsProviderUtil.createCredentialsProvider(Some(accessKeyId), Some(secretAccessKey))

      var receivePid = ""

      val rp = new IRecordProcessor {
        override def initialize(initializationInput: InitializationInput): Unit = {
          println(s">>>>> $initializationInput")
        }
        override def processRecords(processRecordsInput: ProcessRecordsInput): Unit = {
          val records = processRecordsInput.getRecords
            .iterator()
            .asScala
            .map(_.asInstanceOf[RecordAdapter])
            .map(_.getInternalObject)
            .map(_.getDynamodb)
            .toArray

          records
            .foreach { dynamoDb =>
              val newImage = dynamoDb.getNewImage.asScala
              val pid      = newImage("persistence-id").getS
              val message  = newImage("message").getB
              println(s">>>>> $pid, $message")
              receivePid = pid
            }
        }
        override def shutdown(shutdownInput: ShutdownInput): Unit = {}
      }

      val streamArn: String = amazonDynamoDB.describeTable(journalTableName).getTable.getLatestStreamArn

      val accountEventRouterRef = spawn(
        AccountEventRouter(
          id,
          amazonDynamoDB,
          dynamoDBStreamsClient = amazonDynamoDBStreams,
          amazonCloudWatchClient = amazonCloudwatch,
          awsCredentialsProvider = credentialsProvider,
          timestampAtInitialPositionInStream = None,
          regionName = None,
          recordProcessorFactory = Some(
            new IRecordProcessorFactory {
              override def createProcessor(): IRecordProcessor = rp
            }
          ),
          config = config
        )
      )
      val accountId    = AccountId()
      val aggregateRef = spawn(AccountAggregate(accountId))
      val name         = AccountName("test")
      val emailAddress = EmailAddress("test@test.com")

      accountEventRouterRef ! Start(streamArn)

      Thread.sleep(10 * 1000)

      val createSystemAccountReply = createAccount(aggregateRef, maxDuration)(accountId, name, emailAddress)
        .asInstanceOf[CreateAccountSucceeded]
      createSystemAccountReply.accountId shouldBe accountId

      eventually {
        assert(receivePid.nonEmpty)
      }

      accountEventRouterRef.ask[Stopped](ref => StopWithReply(ref)).futureValue
    }
  }

}
