package com.github.j5ik2o.motherbase.interfaceAdaptor.actor

import com.dimafeng.testcontainers.{ FixedHostPortGenericContainer, KafkaContainer }
import net.manub.embeddedkafka.{ EmbeddedKafka, EmbeddedKafkaConfig }
import org.scalatest.{ BeforeAndAfterAll, TestSuite }
import org.testcontainers.containers.wait.strategy.Wait

trait KafkaSpecSupport extends BeforeAndAfterAll {
  this: TestSuite =>

  def zooKeeperPort: Int

  def kafkaPort: Int

  def kafkaConsumerGroupId: String

  def topicName: String

  def customBrokerProperties: Map[String, String] = Map("num.partitions" -> "10")

  def customProducerProperties: Map[String, String] = Map.empty

  def customConsumerProperties: Map[String, String] = Map.empty

//  protected lazy val zooKeeperContainer = new FixedHostPortGenericContainer(
//    imageName = "zookeeper:3.4.9",
//    exposedPorts = Seq(2181),
//    env = Map("ZOO_MY_ID" -> "1", "ZOO_PORT" -> "2181", "ZOO_SERVERS" -> "server.1=zookeeper1:2888:3888"),
//    command = Seq(),
//    classpathResourceMapping = Seq(),
//    waitStrategy = None,
//    exposedHostPort = zooKeeperPort,
//    exposedContainerPort = 2181
//  ).configure { c => c.withNetworkAliases("zookeeper1") }
//
//  protected lazy val kafkaContainer = new FixedHostPortGenericContainer(
//    imageName = "wurstmeister/kafka:2.12-2.4.1",
//    exposedPorts = Seq(9092),
//    env = Map(
//      "KAFKA_AUTO_CREATE_TOPICS_ENABLE"        -> "false",
//      "KAFKA_CREATE_TOPICS"                    -> s"$topicName:128:1",
//      "KAFKA_BROKER_ID"                        -> "1",
//      "KAFKA_ADVERTISED_LISTENERS"             -> "LISTENER_DOCKER_INTERNAL://kafka1:19092,LISTENER_DOCKER_EXTERNAL://${DOCKER_HOST_IP:-127.0.0.1}:9092",
//      "KAFKA_LISTENERS"                        -> "LISTENER_DOCKER_INTERNAL://:19092,LISTENER_DOCKER_EXTERNAL://:9092",
//      "KAFKA_LISTENER_SECURITY_PROTOCOL_MAP"   -> "LISTENER_DOCKER_INTERNAL:PLAINTEXT,LISTENER_DOCKER_EXTERNAL:PLAINTEXT",
//      "KAFKA_INTER_BROKER_LISTENER_NAME"       -> "LISTENER_DOCKER_INTERNAL",
//      "KAFKA_ZOOKEEPER_CONNECT"                -> "zookeeper1:2181",
//      "KAFKA_LOG4J_LOGGERS"                    -> "kafka.controller=INFO,kafka.producer.async.DefaultEventHandler=INFO,state.change.logger=INFO",
//      "KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR" -> "1"
//    ),
//    command = Seq(),
//    classpathResourceMapping = Seq(),
//    waitStrategy = None,
//    exposedHostPort = kafkaPort,
//    exposedContainerPort = 9092
//  ).configure { c => c.withNetworkAliases("kafka1") }

  implicit def kafkaConfig: EmbeddedKafkaConfig = EmbeddedKafkaConfig(
    kafkaPort,
    zooKeeperPort,
    customBrokerProperties,
    customProducerProperties,
    customConsumerProperties
  )

  override def beforeAll(): Unit = {
    super.beforeAll()
    EmbeddedKafka.start()
  }

  override def afterAll(): Unit = {
    EmbeddedKafka.stop()
    super.afterAll()
  }
}
