package com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.readModelUpdater

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.AskPattern._
import akka.stream.scaladsl.Flow
import com.dimafeng.testcontainers.{ ForAllTestContainer, MultipleContainers }
import com.github.j5ik2o.ak.kcl.stage.CommittableRecord
import com.github.j5ik2o.motherbase.accounts.domain.accounts.{ AccountEvents, AccountId, AccountName, EmailAddress }
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.aggregate.AccountProtocol.CreateAccountSucceeded
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.aggregate._
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.router.AccountEventRouter.{
  Start,
  StopWithReply,
  Stopped
}
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.router.{ AccountEventRouter, AccountEventRouterSpec }
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
import net.manub.embeddedkafka.EmbeddedKafka
import org.scalatest.freespec.AnyFreeSpecLike

import scala.concurrent.{ ExecutionContext, Future }

object AccountReadModelUpdaterSpec {
  val defaultAwsAccessKeyId = "x"
  val defaultAwsSecretKey   = "x"
  val dynamoDbPort: Int     = RandomPortUtil.temporaryServerPort()
  val zkPort: Int           = RandomPortUtil.temporaryServerPort()
  val kafkaPort: Int        = RandomPortUtil.temporaryServerPort()
  val kafkaConsumerGroupId  = "motherbase.rmu.account"
  val topic                 = "motherbase.account"
  val minioAccessKeyId      = "AKIAIOSFODNN7EXAMPLE"
  val minioSecretAccessKey  = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY"
  val minioPort             = RandomPortUtil.temporaryServerPort()
  val journalTableName      = "sagrada-journal"
  val readModelTableName    = "sagrada-account"
  val useAwsEnv             = false
  val cloudwatchPort        = RandomPortUtil.temporaryServerPort()
}

class AccountReadModelUpdaterSpec
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
                                                 |      endpoint = "http://127.0.0.1:${AccountReadModelUpdaterSpec.dynamoDbPort}/"
                                                 |    }
                                                 |  }
                                                 |  s3-snapshot-store {
                                                 |    motherbase.bucket-name = "motherbase-test"
                                                 |    class = "com.github.j5ik2o.akka.persistence.s3.snapshot.S3SnapshotStore"
                                                 |    bucket-name-resolver-class-name = "com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.aggregate.MotherBaseBucketNameResolver"
                                                 |    extension-name = "snapshot"
                                                 |    max-load-attempts = 3
                                                 |    s3-client {
                                                 |      access-key-id = "${AccountReadModelUpdaterSpec.minioAccessKeyId}"
                                                 |      secret-access-key = "${AccountReadModelUpdaterSpec.minioSecretAccessKey}"
                                                 |      endpoint = "http://127.0.0.1:${AccountReadModelUpdaterSpec.minioPort}"
                                                 |      s3-options {
                                                 |        path-style-access-enabled = true
                                                 |      }
                                                 |    }
                                                 |  }
                                                 |}
                                                 |motherbase.event-router.accounts {
                                                 |  access-key-id = "${AccountReadModelUpdaterSpec.defaultAwsAccessKeyId}"
                                                 |  secret-access-key = "${AccountReadModelUpdaterSpec.defaultAwsSecretKey}"
                                                 |  dynamodb-client {
                                                 |    access-key-id = "${AccountReadModelUpdaterSpec.defaultAwsAccessKeyId}"
                                                 |    secret-access-key = "${AccountReadModelUpdaterSpec.defaultAwsSecretKey}"
                                                 |    endpoint = "http://127.0.0.1:${AccountReadModelUpdaterSpec.dynamoDbPort}/"
                                                 |  }
                                                 |  dynamodb-stream-client {
                                                 |    access-key-id = "${AccountReadModelUpdaterSpec.defaultAwsAccessKeyId}"
                                                 |    secret-access-key = "${AccountReadModelUpdaterSpec.defaultAwsSecretKey}"
                                                 |    endpoint = "http://127.0.0.1:${AccountReadModelUpdaterSpec.dynamoDbPort}/"
                                                 |  }
                                                 |  cloudwatch-client {
                                                 |    access-key-id = "${AccountReadModelUpdaterSpec.defaultAwsAccessKeyId}"
                                                 |    secret-access-key = "${AccountReadModelUpdaterSpec.defaultAwsSecretKey}"
                                                 |    endpoint = "http://127.0.0.1:${AccountReadModelUpdaterSpec.cloudwatchPort}/"
                                                 |  }
                                                 |  application-name = "test"
                                                 |  initial-position-in-stream = "TRIM_HORIZON"
                                                 |  producer {
                                                 |    discovery-method = akka.discovery
                                                 |    service-name = ""
                                                 |    resolve-timeout = 3 seconds
                                                 |    parallelism = 100
                                                 |    close-timeout = 60s
                                                 |    close-on-producer-stop = true
                                                 |    use-dispatcher = "akka.kafka.default-dispatcher"
                                                 |    eos-commit-interval = 100ms
                                                 |    kafka-clients {
                                                 |      bootstrap.servers = "localhost:${AccountReadModelUpdaterSpec.kafkaPort}"
                                                 |    }
                                                 |  }
                                                 |}
                                                 |motherbase.read-model-updater.accounts {
                                                 |  topic = "${AccountReadModelUpdaterSpec.topic}"
                                                 |  parallelism = 8
                                                 |  committer-settings {
                                                 |    max-batch = 1000
                                                 |    max-interval = 10s
                                                 |    parallelism = 8
                                                 |    delivery = WaitForAck
                                                 |    when = OffsetFirstObserved
                                                 |  }
                                                 |  consumer {
                                                 |    discovery-method = akka.discovery
                                                 |    service-name = ""
                                                 |    resolve-timeout = 3 seconds
                                                 |    poll-interval = 50ms
                                                 |    poll-timeout = 50ms
                                                 |    stop-timeout = 0s
                                                 |    close-timeout = 20s
                                                 |    commit-timeout = 15s
                                                 |    commit-time-warning = 1s
                                                 |    commit-refresh-interval = infinite
                                                 |    use-dispatcher = "akka.kafka.default-dispatcher"
                                                 |    kafka-clients {
                                                 |      enable.auto.commit = false
                                                 |      auto.offset.reset = earliest
                                                 |      bootstrap.servers = "localhost:${AccountReadModelUpdaterSpec.kafkaPort}"
                                                 |      group.id = "${AccountReadModelUpdaterSpec.kafkaConsumerGroupId}"
                                                 |    }
                                                 |    wait-close-partition = 500ms
                                                 |    position-timeout = 5s
                                                 |    offset-for-times-timeout = 5s
                                                 |    metadata-request-timeout = 5s
                                                 |    eos-draining-check-interval = 30ms
                                                 |    partition-handler-warning = 5s
                                                 |    connection-checker {
                                                 |      enable = false
                                                 |      max-retries = 3
                                                 |      check-interval = 15s
                                                 |      backoff-factor = 2.0
                                                 |    }
                                                 |  }
                                                 |}
                                                 |
                                                 |""".stripMargin).withFallback(ConfigFactory.load()))
    with AnyFreeSpecLike
    with ForAllTestContainer
    with DynamoDbSpecSupport
    with JournalTableSpecSupport
    with S3SpecSupport
    with LocalStackSpecSupport
    with KafkaSpecSupport
    with EnrichKafkaFuture
    with AggregateSpecScenarioBase
    with AccountAggregateSpecHelper {

  override def zooKeeperPort: Int               = AccountReadModelUpdaterSpec.zkPort
  override def kafkaPort: Int                   = AccountReadModelUpdaterSpec.kafkaPort
  override def kafkaConsumerGroupId: String     = AccountReadModelUpdaterSpec.kafkaConsumerGroupId
  def topicName: String                         = AccountReadModelUpdaterSpec.topic
  override protected lazy val dynamoDBPort: Int = AccountReadModelUpdaterSpec.dynamoDbPort

  override protected def minioAccessKeyId: String     = AccountReadModelUpdaterSpec.minioAccessKeyId
  override protected def minioSecretAccessKey: String = AccountReadModelUpdaterSpec.minioSecretAccessKey
  override protected def minioPort: Int               = AccountReadModelUpdaterSpec.minioPort

  override protected def localStackPort: Int = AccountEventRouterSpec.cloudwatchPort

  override protected def s3BucketName(system: ActorSystem[_]): String =
    new MotherBaseBucketNameResolver(system.settings.config.getConfig("j5ik2o.s3-snapshot-store")).resolve(null)

  override lazy val container =
    MultipleContainers(dynamoDbLocalContainer, minioContainer, localStackContainer)

  override def afterStart(): Unit = {
    implicit val ec = system.executionContext
    createS3Bucket()
    createJournalTable()

  }

  override def beforeStop() = {
    deleteJournalTable()
  }

  "AccountReadModelUpdaterSpec" - {
    "routing & read model update" in {
      val rootConfig                           = system.settings.config
      val id                                   = ULID()
      val config                               = rootConfig.getConfig("motherbase.event-router.accounts")
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

      EmbeddedKafka.createCustomTopic(
        AccountReadModelUpdaterSpec.topic,
        partitions = customBrokerProperties("num.partitions").toInt
      )

      var resultEvent: AccountEvents.AccountCreated = null

      val accountEventWriter = new AccountEventWriter {
        override def create(events: Seq[AccountEvents.AccountCreated])(implicit ec: ExecutionContext): Future[Int] =
          Future.successful {
            0
          }

        override def create(event: AccountEvents.AccountCreated)(implicit ec: ExecutionContext): Future[Unit] =
          Future.successful {
            resultEvent = event
            1
          }
      }
      val rmu =
        new AccountReadModelUpdater(rootConfig.getConfig("motherbase.read-model-updater.accounts"), accountEventWriter)(
          system.classicSystem
        )
      val rmuRef = spawn(rmu.behavior)
      rmuRef ! AccountReadModelUpdater.Start(None)

      val streamArn: String = amazonDynamoDB.describeTable(journalTableName).getTable.getLatestStreamArn
      val accountEventRouterRef = spawn(
        AccountEventRouter(
          id,
          amazonDynamoDB,
          dynamoDBStreamsClient = amazonDynamoDBStreams,
          amazonCloudWatchClient = amazonCloudwatch,
          awsCredentialsProvider = credentialsProvider,
          producerFlow = AccountEventRouter
            .kafkaProducerFlow(
              AccountReadModelUpdaterSpec.topic,
              rootConfig.getConfig("motherbase.event-router.accounts")
            ),
          timestampAtInitialPositionInStream = None,
          regionName = None,
          config = config
        )
      )
      val accountId    = AccountId()
      val aggregateRef = spawn(AccountAggregate(accountId))
      val name         = AccountName("test")
      val emailAddress = EmailAddress("test@test.com")

      accountEventRouterRef ! AccountEventRouter.Start(streamArn)

      Thread.sleep(10 * 1000)

      val createSystemAccountReply = createAccount(aggregateRef, maxDuration)(accountId, name, emailAddress)
        .asInstanceOf[CreateAccountSucceeded]
      createSystemAccountReply.accountId shouldBe accountId

      eventually {
        assert(resultEvent != null)
        println(s"resultEvent = $resultEvent")
      }

      accountEventRouterRef.ask[Stopped](ref => StopWithReply(ref)).futureValue
    }
  }

}
