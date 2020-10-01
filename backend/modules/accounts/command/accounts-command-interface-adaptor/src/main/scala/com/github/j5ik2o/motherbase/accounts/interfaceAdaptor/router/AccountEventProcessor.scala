package com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.router

import akka.NotUsed
import akka.actor.typed.scaladsl.ActorContext
import akka.kafka.scaladsl.Producer
import akka.kafka.{ ProducerMessage, ProducerSettings }
import akka.serialization.{ Serialization, SerializationExtension }
import akka.stream.scaladsl.{ Sink, Source }
import com.amazonaws.services.dynamodbv2.streamsadapter.model.RecordAdapter
import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.IRecordProcessor
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.ShutdownReason
import com.amazonaws.services.kinesis.clientlibrary.types.{ InitializationInput, ProcessRecordsInput, ShutdownInput }
import com.typesafe.config.Config
import org.apache.kafka.clients.producer.{ ProducerRecord, Producer => KafkaProducer }
import org.apache.kafka.common.serialization.{ ByteArraySerializer, StringSerializer }
import org.slf4j.LoggerFactory

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.jdk.CollectionConverters._

final class AccountEventProcessor(ctx: ActorContext[_], config: Config) extends IRecordProcessor {

  implicit val system        = ctx.system
  private val topic: String  = config.getString("topic")
  private val producerConfig = config.getConfig("producer")
  private val logger         = LoggerFactory.getLogger(getClass)

  val producerSettings: ProducerSettings[String, Array[Byte]] =
    ProducerSettings(producerConfig, new StringSerializer, new ByteArraySerializer)
  val producer: KafkaProducer[String, Array[Byte]] = producerSettings.createKafkaProducer()
  val serialization: Serialization                 = SerializationExtension(ctx.system)
  private var checkpointCounter                    = 0
  private var shardId: String                      = _

  override def initialize(initializationInput: InitializationInput): Unit = {
    checkpointCounter = 0
    shardId = initializationInput.getShardId
  }

  override def processRecords(processRecordsInput: ProcessRecordsInput): Unit = {
    val startTimestamp = System.currentTimeMillis()
    logger.info(s"Received a ddbEvent")

    val records = processRecordsInput.getRecords
      .iterator()
      .asScala
      .map(_.asInstanceOf[RecordAdapter])
      .map(_.getInternalObject)
      .map(_.getDynamodb)
      .toArray

    val firstJournalTimestamp = records.head.getNewImage.asScala.apply("ordering").getN.toLong
    val lastJournalTimestamp  = records.last.getNewImage.asScala.apply("ordering").getN.toLong
    val averageTimeStamp =
      records.iterator.map(_.getNewImage.asScala.apply("ordering").getN.toLong).sum / records.length

    logger.info(s"Latency from FirstJournal: ${startTimestamp - firstJournalTimestamp} ms")
    logger.info(s"Latency from LastJournal: ${startTimestamp - lastJournalTimestamp} ms")
    logger.info(s"Average Latency from Journal: ${startTimestamp - averageTimeStamp} ms")

    val producerRecords = records
      .map { dynamoDb =>
        val newImage = dynamoDb.getNewImage.asScala
        val pid      = newImage("persistence-id").getS
        val message  = newImage("message").getB
        new ProducerRecord[String, Array[Byte]](topic, null, pid, message.array())
      }
    val producerMessage: ProducerMessage.Envelope[String, Array[Byte], NotUsed] = ProducerMessage.multi(producerRecords)
    val producerRecordsConvertTimestamp                                         = System.currentTimeMillis()

    logger.info(s"Duration of Converting Journal: ${producerRecordsConvertTimestamp - startTimestamp} ms")

    val future = Source
      .single(producerMessage)
      .via(Producer.flexiFlow(producerSettings))
      .runWith(Sink.ignore)

    try {
      val results = Await.result(future, Duration.Inf)
      processRecordsInput.getCheckpointer.checkpoint()
      logger.debug(s">>> results = $results")

      val endTimestamp = System.currentTimeMillis()

      logger.info(s"Duration of KafkaProduce: ${endTimestamp - producerRecordsConvertTimestamp} ms")
      logger.info(s"Duration of handleRequest: ${endTimestamp - startTimestamp} ms")
      logger.info(s"Successfully processed " + producerRecords.length + " records.")
    } catch {
      case ex: Exception =>
        logger.error("occurred error", ex)
        throw ex
    }

  }

  override def shutdown(shutdownInput: ShutdownInput): Unit = {
    if (shutdownInput.getShutdownReason == ShutdownReason.TERMINATE) {
      try {
        shutdownInput.getCheckpointer.checkpoint()
      } catch {
        case ex: Exception =>
          logger.error("occurred error", ex)
      }
      try {
        producer.close()
      } catch {
        case ex: Exception =>
          logger.error("occurred error", ex)
      }
    }
  }
}
