package com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.aggregate

import akka.actor.typed.{ActorSystem, Behavior}
import com.dimafeng.testcontainers.{ForAllTestContainer, MultipleContainers}
import com.github.j5ik2o.motherbase.accounts.domain.system.SystemAccountId
import com.github.j5ik2o.motherbase.interfaceAdaptor.actor.{ActorSpec, DynamoDbSpecSupport, JournalTableSpecSupport, S3SpecSupport}
import com.github.j5ik2o.motherbase.interfaceAdaptor.util.RandomPortUtil
import com.typesafe.config.ConfigFactory

object SystemAccountAggregateOnDynamoDBSpec {
  val dynamoDbPort: Int    = RandomPortUtil.temporaryServerPort()
  val minioAccessKeyId     = "AKIAIOSFODNN7EXAMPLE"
  val minioSecretAccessKey = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY"
  val minioPort            = RandomPortUtil.temporaryServerPort()
}

class SystemAccountAggregateOnDynamoDBSpec
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
                                                 |      endpoint = "http://127.0.0.1:${SystemAccountAggregateOnDynamoDBSpec.dynamoDbPort}/"
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
                                                 |      access-key-id = "${SystemAccountAggregateOnDynamoDBSpec.minioAccessKeyId}"
                                                 |      secret-access-key = "${SystemAccountAggregateOnDynamoDBSpec.minioSecretAccessKey}"
                                                 |      endpoint = "http://127.0.0.1:${SystemAccountAggregateOnDynamoDBSpec.minioPort}"
                                                 |      s3-options {
                                                 |        path-style-access-enabled = true
                                                 |      }
                                                 |    }
                                                 |  }
                                                 |}
                                                 |""".stripMargin).withFallback(ConfigFactory.load()))
    with SystemAccountAggregateSpecScenario
    with ForAllTestContainer
    with DynamoDbSpecSupport
    with JournalTableSpecSupport
    with S3SpecSupport {

  override protected lazy val dynamoDBPort: Int = SystemAccountAggregateOnDynamoDBSpec.dynamoDbPort

  override protected def minioAccessKeyId: String     = SystemAccountAggregateOnDynamoDBSpec.minioAccessKeyId
  override protected def minioSecretAccessKey: String = SystemAccountAggregateOnDynamoDBSpec.minioSecretAccessKey
  override protected def minioPort: Int               = SystemAccountAggregateOnDynamoDBSpec.minioPort

  override val container = MultipleContainers(dynamoDbLocalContainer, minioContainer)

  override def afterStart(): Unit = {
    implicit val ec = system.executionContext
    createS3Bucket()
    createJournalTable()
  }

  override def beforeStop() = {
    deleteJournalTable()
  }

  override def s3BucketName(system: ActorSystem[_]): String =
    new MotherBaseBucketNameResolver(system.settings.config.getConfig("j5ik2o.s3-snapshot-store")).resolve(null)

  override def behavior(systemAccountId: SystemAccountId): Behavior[SystemAccountProtocol.Command] =
    SystemAccountAggregate(systemAccountId)
}
