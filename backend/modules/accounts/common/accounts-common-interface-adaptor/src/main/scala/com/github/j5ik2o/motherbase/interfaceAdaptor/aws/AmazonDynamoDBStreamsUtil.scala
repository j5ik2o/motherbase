package com.github.j5ik2o.motherbase.interfaceAdaptor.aws

import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.{ AWSCredentialsProvider, AWSStaticCredentialsProvider, BasicAWSCredentials }
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.regions.Regions
import com.amazonaws.services.dynamodbv2.{ AmazonDynamoDBStreams, AmazonDynamoDBStreamsClientBuilder }
import com.typesafe.config.Config
import net.ceedubs.ficus.Ficus._

object AmazonDynamoDBStreamsUtil {

  def createFromConfig(config: Config): AmazonDynamoDBStreams = {
    val awsRegion: Regions =
      config.getAs[String]("region").map(s => Regions.fromName(s)).getOrElse(Regions.AP_NORTHEAST_1)
    val dynamoDBStreamsAccessKeyId: Option[String]     = config.getAs[String]("access-key-id")
    val dynamoDBStreamsSecretAccessKey: Option[String] = config.getAs[String]("secret-access-key")
    val dynamoDBStreamsEndpoint: Option[String]        = config.getAs[String]("endpoint")
    val clientConfigurationConfig                      = config.getAs[Config]("client-configuration")
    create(
      awsRegion,
      dynamoDBStreamsAccessKeyId,
      dynamoDBStreamsSecretAccessKey,
      dynamoDBStreamsEndpoint,
      clientConfigurationConfig.map(ccc => ClientConfigurationUtil.createFromConfig(ccc))
    )
  }

  def create(
      awsRegion: Regions,
      accessKeyId: Option[String],
      secretAccessKey: Option[String],
      endpoint: Option[String],
      clientConfiguration: Option[ClientConfiguration]
  ): AmazonDynamoDBStreams = {
    val builder = AmazonDynamoDBStreamsClientBuilder.standard
    (accessKeyId, secretAccessKey, endpoint) match {
      case (Some(aki), Some(sak), Some(e)) =>
        val awsCredentialsProvider: AWSCredentialsProvider = new AWSStaticCredentialsProvider(
          new BasicAWSCredentials(aki, sak)
        )
        clientConfiguration
          .fold(builder) { cc => builder.withClientConfiguration(cc) }
          .withCredentials(awsCredentialsProvider)
          .withEndpointConfiguration(new EndpointConfiguration(e, awsRegion.getName))
          .build()
      case _ =>
        clientConfiguration
          .fold(builder) { cc => builder.withClientConfiguration(cc) }
          .build()
    }
  }
}
