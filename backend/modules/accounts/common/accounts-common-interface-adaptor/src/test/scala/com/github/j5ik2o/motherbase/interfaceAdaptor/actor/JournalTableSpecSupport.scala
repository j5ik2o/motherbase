package com.github.j5ik2o.motherbase.interfaceAdaptor.actor

import com.github.j5ik2o.reactive.aws.dynamodb.implicits._
import software.amazon.awssdk.services.dynamodb.model._

trait JournalTableSpecSupport { this: ActorSpec with DynamoDbSpecSupport =>

  def journalTableName: String      = "Journal"
  def isDeleteJournalTable: Boolean = true
  def isCreateJournalTable: Boolean = true

  protected def deleteJournalTable(): Unit = {
    if (isDeleteJournalTable) {
      dynamoDbAsyncClient.deleteTable(journalTableName)
      eventually {
        val result = dynamoDbAsyncClient.listTables(ListTablesRequest.builder().limit(1).build()).futureValue
        result.tableNamesAsScala.fold(true)(v => !v.contains(journalTableName)) shouldBe true
      }
    }
  }

  protected def createJournalTable(): Unit = {
    if (isCreateJournalTable && !dynamoDbAsyncClient
          .listTables().futureValue.tableNamesAsScala.get.contains(journalTableName)) {
      val createRequest = CreateTableRequest
        .builder()
        .tableName(journalTableName)
        .attributeDefinitionsAsScala(
          Seq(
            AttributeDefinition
              .builder()
              .attributeName("pkey")
              .attributeType(ScalarAttributeType.S).build(),
            AttributeDefinition
              .builder()
              .attributeName("skey")
              .attributeType(ScalarAttributeType.S).build(),
            AttributeDefinition
              .builder()
              .attributeName("persistence-id")
              .attributeType(ScalarAttributeType.S).build(),
            AttributeDefinition
              .builder()
              .attributeName("sequence-nr")
              .attributeType(ScalarAttributeType.N).build(),
            AttributeDefinition
              .builder()
              .attributeName("tags")
              .attributeType(ScalarAttributeType.S).build()
          )
        )
        .keySchemaAsScala(
          Seq(
            KeySchemaElement
              .builder()
              .attributeName("pkey")
              .keyType(KeyType.HASH).build(),
            KeySchemaElement
              .builder()
              .attributeName("skey")
              .keyType(KeyType.RANGE).build()
          )
        )
        .provisionedThroughput(
          ProvisionedThroughput
            .builder()
            .readCapacityUnits(10L)
            .writeCapacityUnits(10L).build()
        )
        .globalSecondaryIndexesAsScala(
          Seq(
            GlobalSecondaryIndex
              .builder()
              .indexName("TagsIndex")
              .keySchemaAsScala(
                Seq(
                  KeySchemaElement.builder().keyType(KeyType.HASH).attributeName("tags").build()
                )
              ).projection(Projection.builder().projectionType(ProjectionType.ALL).build())
              .provisionedThroughput(
                ProvisionedThroughput
                  .builder()
                  .readCapacityUnits(10L)
                  .writeCapacityUnits(10L).build()
              ).build(),
            GlobalSecondaryIndex
              .builder()
              .indexName("GetJournalRowsIndex").keySchemaAsScala(
                Seq(
                  KeySchemaElement.builder().keyType(KeyType.HASH).attributeName("persistence-id").build(),
                  KeySchemaElement.builder().keyType(KeyType.RANGE).attributeName("sequence-nr").build()
                )
              ).projection(Projection.builder().projectionType(ProjectionType.ALL).build())
              .provisionedThroughput(
                ProvisionedThroughput
                  .builder()
                  .readCapacityUnits(10L)
                  .writeCapacityUnits(10L).build()
              ).build()
          )
        )
        .streamSpecification(
          StreamSpecification.builder().streamEnabled(true).streamViewType(StreamViewType.NEW_IMAGE).build()
        )
        .build()

      val createResponse = dynamoDbAsyncClient
        .createTable(createRequest).futureValue
      createResponse.sdkHttpResponse().isSuccessful shouldBe true

    }
  }
}
