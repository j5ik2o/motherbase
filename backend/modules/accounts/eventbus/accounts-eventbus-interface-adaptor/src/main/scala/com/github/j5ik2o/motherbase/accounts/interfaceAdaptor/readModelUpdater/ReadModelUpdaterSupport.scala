package com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.readModelUpdater

import java.time.{ Duration => JDuration }
import java.util.Properties

import akka.kafka.ConsumerSettings
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.admin.AdminClient

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.jdk.CollectionConverters._

trait ReadModelUpdaterSupport extends EnrichKafkaFuture {

  // KafkaAdmin で パーティション数を取得する
  def retrievePartitionCounts[K, V](
      topic: String,
      consumerSettings: ConsumerSettings[K, V],
      timeout: FiniteDuration
  ): Int = {
    lazy val admin: AdminClient = {
      val props = new Properties()
      props.put(
        CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG,
        consumerSettings.getProperty(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG)
      )
      AdminClient.create(props)
    }
    try {
      val descriptionFuture = admin
        .describeTopics(Seq(topic).asJava)
        .values().asScala
        .apply(topic).asScala

      val description = Await.result(descriptionFuture, timeout)

      description
        .partitions()
        .size()
    } finally {
      admin.close(JDuration.ofMillis(timeout.toMillis))
    }
  }
}
