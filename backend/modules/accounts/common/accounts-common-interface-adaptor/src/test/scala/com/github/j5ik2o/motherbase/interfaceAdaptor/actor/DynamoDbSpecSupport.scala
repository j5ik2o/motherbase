package com.github.j5ik2o.motherbase.interfaceAdaptor.actor

import java.net.URI

import com.dimafeng.testcontainers.FixedHostPortGenericContainer
import com.github.j5ik2o.motherbase.interfaceAdaptor.util.RandomPortUtil
import com.github.j5ik2o.reactive.aws.dynamodb.DynamoDbAsyncClient
import org.testcontainers.containers.wait.strategy.Wait
import software.amazon.awssdk.auth.credentials.{ AwsBasicCredentials, StaticCredentialsProvider }
import software.amazon.awssdk.services.dynamodb.{ DynamoDbAsyncClient => JavaDynamoDbAsyncClient }

trait DynamoDbSpecSupport {
  protected def useAwsEnv                        = false
  protected lazy val dynamoDBAccessKeyId: String = "x"

  protected lazy val dynamoDBSecretAccessKey: String = "x"

  protected lazy val dynamoDBPort: Int = RandomPortUtil.temporaryServerPort()

  protected lazy val dynamoDBEndpoint: String = s"http://127.0.0.1:$dynamoDBPort"

  protected lazy val dynamoDbLocalContainer: FixedHostPortGenericContainer = FixedHostPortGenericContainer(
    "amazon/dynamodb-local:latest",
    exposedHostPort = dynamoDBPort,
    exposedContainerPort = 8000,
    waitStrategy = Wait.forListeningPort()
  )

  def underlyingAsyncV2: JavaDynamoDbAsyncClient =
    if (!useAwsEnv) {
      println("v2 async = " + (dynamoDBEndpoint, dynamoDBAccessKeyId, dynamoDBSecretAccessKey))
      JavaDynamoDbAsyncClient
        .builder()
        .credentialsProvider(
          StaticCredentialsProvider.create(AwsBasicCredentials.create(dynamoDBAccessKeyId, dynamoDBSecretAccessKey))
        )
        .endpointOverride(URI.create(dynamoDBEndpoint))
        .build()
    } else {
      JavaDynamoDbAsyncClient
        .builder()
        .build()
    }

  def dynamoDbAsyncClient: DynamoDbAsyncClient = DynamoDbAsyncClient(underlyingAsyncV2)
}
