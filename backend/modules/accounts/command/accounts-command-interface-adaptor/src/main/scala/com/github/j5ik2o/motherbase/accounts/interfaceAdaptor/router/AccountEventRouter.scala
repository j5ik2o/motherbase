package com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.router

import java.time.Instant
import java.util.Date

import net.ceedubs.ficus.Ficus._
import akka.actor.typed.{ ActorRef, Behavior }
import akka.actor.typed.scaladsl.{ AbstractBehavior, ActorContext, Behaviors }
import akka.kafka.scaladsl.Producer
import akka.kafka.{ ProducerMessage, ProducerSettings }
import akka.serialization.{ Serialization, SerializationExtension }
import akka.stream.scaladsl.{ Sink, Source }
import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import com.amazonaws.services.dynamodbv2.model.{ BillingMode, DescribeStreamRequest }
import com.amazonaws.services.dynamodbv2.streamsadapter.{ AmazonDynamoDBStreamsAdapterClient, StreamsWorkerFactory }
import com.amazonaws.services.dynamodbv2.streamsadapter.model.RecordAdapter
import com.amazonaws.services.dynamodbv2.{ AmazonDynamoDB, AmazonDynamoDBStreams }
import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.{ IRecordProcessor, IRecordProcessorFactory }
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.{
  InitialPositionInStream,
  KinesisClientLibConfiguration,
  NoOpShardPrioritization,
  ShardSyncStrategyType,
  ShutdownReason,
  Worker
}
import com.amazonaws.services.kinesis.clientlibrary.types.{ InitializationInput, ProcessRecordsInput, ShutdownInput }
import com.amazonaws.services.kinesis.metrics.interfaces.MetricsLevel
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.router.AccountEventRouter.{
  Command,
  Start,
  StartWithReply,
  Started,
  WrappedResult
}
import com.github.j5ik2o.motherbase.infrastructure.ulid.ULID
import com.typesafe.config.Config
import kamon.Kamon
import org.apache.kafka.clients.producer.{ ProducerRecord, Producer => KafkaProducer }
import org.apache.kafka.common.serialization.{ ByteArraySerializer, StringSerializer }
import org.slf4j.LoggerFactory

import scala.concurrent.{ Await, Promise }
import scala.concurrent.duration.Duration
import scala.jdk.CollectionConverters._
import scala.concurrent.duration._
import scala.util.{ Failure, Success }

object AccountEventRouter {
  trait Command
  trait CommandReply
  case class StartWithReply(streamArn: String, replyTo: ActorRef[CommandReply]) extends Command
  case class Start(streamArn: String)                                           extends Command
  case object Started                                                           extends CommandReply
  case object Stop                                                              extends Command
  case class Stopped()                                                          extends CommandReply
  private[router] case class WrappedResult(replyTo: ActorRef[CommandReply])     extends Command

  private[router] val kafkaProduceCounter    = Kamon.counter("kafka-produce-count").withTag("event-type", "account")
  private[router] val dynamoDbConsumeCounter = Kamon.counter("dynamo-db-consume-count").withTag("event-type", "account")
  private[router] val processRecordsCounter  = Kamon.counter("process-records-count").withTag("event-type", "account")
}

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
    val producerMessage                 = ProducerMessage.multi(producerRecords)
    val producerRecordsConvertTimestamp = System.currentTimeMillis()

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

final class AccountEventProcessorFactory(ctx: ActorContext[_], config: Config) extends IRecordProcessorFactory {
  override def createProcessor(): IRecordProcessor = new AccountEventProcessor(ctx, config)
}

final class AccountEventRouter(
    id: ULID,
    amazonDynamoDB: AmazonDynamoDB,
    dynamoDBStreamsClient: AmazonDynamoDBStreams,
    amazonCloudWatchClient: AmazonCloudWatch,
    awsCredentialsProvider: AWSCredentialsProvider,
    timestampAtInitialPositionInStream: Option[Instant],
    regionName: Option[String],
    config: Config
)(ctx: ActorContext[Command])
    extends AbstractBehavior[Command](ctx) {

  private val adapterClient: AmazonDynamoDBStreamsAdapterClient =
    new AmazonDynamoDBStreamsAdapterClient(dynamoDBStreamsClient)
  private val recordProcessorFactory = new AccountEventProcessorFactory(ctx, config.getConfig("account"))
  private var worker: Worker         = null
  private var workerThread: Thread   = null

  private def startWorker(streamArn: String) = {
    val describeStreamRequest = new DescribeStreamRequest().withStreamArn(streamArn)
    val result                = dynamoDBStreamsClient.describeStream(describeStreamRequest)
    val shards                = result.getStreamDescription.getShards.asScala
    ctx.log.debug(s"shards.size = ${shards.size}, shards = $shards")
    worker = StreamsWorkerFactory.createDynamoDbStreamsWorker(
      recordProcessorFactory,
      createWorkerConfig(
        id,
        awsCredentialsProvider,
        streamArn,
        timestampAtInitialPositionInStream,
        regionName,
        config
      ),
      adapterClient,
      amazonDynamoDB,
      amazonCloudWatchClient
    )
  }

  override def onMessage(msg: Command): Behavior[Command] = {
    msg match {
      case WrappedResult(replyTo) =>
        replyTo ! Started
        Behaviors.same
      case Start(streamArn) =>
        startWorker(streamArn)
        workerThread = new Thread(worker)
        workerThread.start()
        Behaviors.same
      case StartWithReply(streamArn, replyTo) =>
        startWorker(streamArn)
        val start = Promise[Unit]()
        ctx.pipeToSelf(start.future) {
          case Success(_)  => WrappedResult(replyTo)
          case Failure(ex) => throw ex
        }
        workerThread = new Thread(worker)
        workerThread.start()
        start.success(())
        Behaviors.same
    }
  }

  private def createWorkerConfig(
      id: ULID,
      awsCredentialsProvider: AWSCredentialsProvider,
      streamArn: String,
      timestampAtInitialPositionInStream: Option[Instant],
      regionName: Option[String],
      config: Config
  ): KinesisClientLibConfiguration = {
    val applicationName = config.getString("application-name")
    val position = config.getOrElse[String](
      "initial-position-in-stream",
      KinesisClientLibConfiguration.DEFAULT_INITIAL_POSITION_IN_STREAM.toString
    )
    val maxRecords = config.getOrElse[Int]("max-records", KinesisClientLibConfiguration.DEFAULT_MAX_RECORDS)
    val idleTimeBetweenReads = config.getOrElse(
      "idle-time-between-reads",
      KinesisClientLibConfiguration.DEFAULT_IDLETIME_BETWEEN_READS_MILLIS millis
    )
    val failoverTime =
      config.getOrElse("failover-time", KinesisClientLibConfiguration.DEFAULT_FAILOVER_TIME_MILLIS millis)
    val shardSyncInterval = config
      .getOrElse[Duration](
        "shard-sync-interval",
        KinesisClientLibConfiguration.DEFAULT_SHARD_SYNC_INTERVAL_MILLIS millis
      )
    val callProcessRecordsEvenForEmptyRecordList = config.getOrElse(
      "call-process-records-even-for-empty-record-list",
      KinesisClientLibConfiguration.DEFAULT_DONT_CALL_PROCESS_RECORDS_FOR_EMPTY_RECORD_LIST
    )
    val parentShardPollInterval = config.getOrElse(
      "parent-shard-poll-interval",
      KinesisClientLibConfiguration.DEFAULT_PARENT_SHARD_POLL_INTERVAL_MILLIS millis
    )
    val cleanupLeasesUponShardCompletion = config.getOrElse(
      "cleanup-leases-upon-shard-completion",
      KinesisClientLibConfiguration.DEFAULT_CLEANUP_LEASES_UPON_SHARDS_COMPLETION
    )
    // val withIgnoreUnexpectedChildShards = config.getOrElse("withIgnoreUnexpectedChildShards",KinesisClientLibConfiguration. )
    val userAgent = config.getOrElse("user-agent", KinesisClientLibConfiguration.KINESIS_CLIENT_LIB_USER_AGENT)
    val taskBackoffTime = config.getOrElse(
      "task-backoff-time",
      KinesisClientLibConfiguration.DEFAULT_TASK_BACKOFF_TIME_MILLIS millis
    )
    val metricsBufferTime = config.getOrElse(
      "metrics-buffer-time",
      KinesisClientLibConfiguration.DEFAULT_METRICS_BUFFER_TIME_MILLIS millis
    )
    val metricsMaxQueueSize =
      config.getOrElse("metrics-max-queue-size", KinesisClientLibConfiguration.DEFAULT_METRICS_MAX_QUEUE_SIZE)
    val metricsLevel =
      config.getOrElse("metrics-level", KinesisClientLibConfiguration.DEFAULT_METRICS_LEVEL.getName)
    val billingMode =
      config.getOrElse("billing-mode", KinesisClientLibConfiguration.DEFAULT_DDB_BILLING_MODE.toString)
    val validateSequenceNumberBeforeCheckpointing = config.getOrElse(
      "validate-sequence-number-before-checkpointing",
      KinesisClientLibConfiguration.DEFAULT_VALIDATE_SEQUENCE_NUMBER_BEFORE_CHECKPOINTING
    )
    val skipShardSyncAtStartupIfLeasesExist = config.getOrElse(
      "skip-shard-sync-at-startup-if-leases-exist",
      KinesisClientLibConfiguration.DEFAULT_SKIP_SHARD_SYNC_AT_STARTUP_IF_LEASES_EXIST
    )
    val shardSyncStrategyType = config.getOrElse(
      "shard-sync-strategy-type",
      KinesisClientLibConfiguration.DEFAULT_SHARD_SYNC_STRATEGY_TYPE.toString
    )
    val maxLeasesForWorker =
      config.getOrElse("max-leases-for-worker", KinesisClientLibConfiguration.DEFAULT_MAX_LEASES_FOR_WORKER)
    val maxLeasesToStealAtOneTime = config.getOrElse(
      "max-leases-to-steal-at-one-time",
      KinesisClientLibConfiguration.DEFAULT_MAX_LEASES_TO_STEAL_AT_ONE_TIME
    )
    val initialLeaseTableReadCapacity = config.getOrElse(
      "initial-lease-table-read-capacity",
      KinesisClientLibConfiguration.DEFAULT_INITIAL_LEASE_TABLE_READ_CAPACITY
    )
    val initialLeaseTableWriteCapacity = config.getOrElse(
      "initial-lease-table-write-capacity",
      KinesisClientLibConfiguration.DEFAULT_INITIAL_LEASE_TABLE_WRITE_CAPACITY
    )
    val maxLeaseRenewalThreads =
      config.getOrElse("max-lease-renewal-threads", KinesisClientLibConfiguration.DEFAULT_MAX_LEASE_RENEWAL_THREADS)

    val maxPendingProcessRecordsInput = config.getAs[Int]("max-pending-process-records-input")
    val retryGetRecordsInSeconds      = config.getAs[Duration]("retry-get-records")
    val maxGetRecordsThreadPool       = config.getAs[Int]("max-get-records-thread-pool")
    val maxCacheByteSize              = config.getAs[Int]("max-cache-byte-size")
    val dataFetchingStrategy          = config.getAs[String]("data-fetching-strategy")
    val maxRecordsCount               = config.getAs[Int]("max-records-count")
    val timeout                       = config.getAs[Duration]("timeout")
    val shutdownGrace =
      config.getOrElse("shutdown-grace", KinesisClientLibConfiguration.DEFAULT_SHUTDOWN_GRACE_MILLIS millis)
    val idleMillisBetweenCalls       = config.getAs[Long]("idle-millis-between-calls")
    val logWarningForTaskAfterMillis = config.getAs[Duration]("log-warning-for-task-after")
    val listShardsBackoffTimeInMillis =
      config.getOrElse(
        "list-shards-backoff-time",
        KinesisClientLibConfiguration.DEFAULT_LIST_SHARDS_BACKOFF_TIME_IN_MILLIS millis
      )
    val maxListShardsRetryAttempts = config.getOrElse(
      "max-list-shards-retry-attempts",
      KinesisClientLibConfiguration.DEFAULT_MAX_LIST_SHARDS_RETRY_ATTEMPTS
    )

    val baseWorkerConfig = new KinesisClientLibConfiguration(
      applicationName,
      streamArn,
      awsCredentialsProvider,
      id.asString
    ).withInitialPositionInStream(InitialPositionInStream.valueOf(position))
      .withFailoverTimeMillis(failoverTime.toMillis)
      .withShardSyncIntervalMillis(shardSyncInterval.toMillis)
      .withMaxRecords(maxRecords)
      .withIdleTimeBetweenReadsInMillis(idleTimeBetweenReads.toMillis)
      .withCallProcessRecordsEvenForEmptyRecordList(callProcessRecordsEvenForEmptyRecordList)
      .withParentShardPollIntervalMillis(parentShardPollInterval.toMillis)
      .withCleanupLeasesUponShardCompletion(cleanupLeasesUponShardCompletion)
      // withIgnoreUnexpectedChildShards
      // withCommonClientConfig
      // withKinesisClientConfig
      // withDynamoDBClientConfig
      // withCloudWatchClientConfig
      .withUserAgent(userAgent)
      .withTaskBackoffTimeMillis(taskBackoffTime.toMillis)
      .withMetricsBufferTimeMillis(metricsBufferTime.toMillis)
      .withMetricsMaxQueueSize(metricsMaxQueueSize)
      .withMetricsLevel(MetricsLevel.valueOf(metricsLevel))
      .withBillingMode(BillingMode.valueOf(billingMode))
      // withMetricsLevel
      // withMetricsEnabledDimensions
      .withValidateSequenceNumberBeforeCheckpointing(validateSequenceNumberBeforeCheckpointing)
      .withSkipShardSyncAtStartupIfLeasesExist(skipShardSyncAtStartupIfLeasesExist)
      .withShardSyncStrategyType(ShardSyncStrategyType.valueOf(shardSyncStrategyType))
      .withMaxLeasesForWorker(maxLeasesForWorker)
      .withMaxLeasesToStealAtOneTime(maxLeasesToStealAtOneTime)
      .withInitialLeaseTableReadCapacity(initialLeaseTableReadCapacity)
      .withInitialLeaseTableWriteCapacity(initialLeaseTableWriteCapacity)
      .withShardPrioritizationStrategy(new NoOpShardPrioritization())
      .withMaxLeaseRenewalThreads(maxLeaseRenewalThreads)
      .withShutdownGraceMillis(shutdownGrace.toMillis)
      .withListShardsBackoffTimeInMillis(listShardsBackoffTimeInMillis.toMillis)
      .withMaxListShardsRetryAttempts(maxListShardsRetryAttempts)

    val c1 = timestampAtInitialPositionInStream.fold(baseWorkerConfig) { instant =>
      baseWorkerConfig.withTimestampAtInitialPositionInStream(Date.from(instant))
    }
    val c2  = regionName.fold(c1) { v => c1.withRegionName(v) }
    val c3  = retryGetRecordsInSeconds.fold(c2) { v => c2.withRetryGetRecordsInSeconds(v.toSeconds.toInt) }
    val c4  = maxGetRecordsThreadPool.fold(c3) { v => c3.withMaxGetRecordsThreadPool(v) }
    val c5  = maxPendingProcessRecordsInput.fold(c4) { v => c4.withMaxPendingProcessRecordsInput(v) }
    val c6  = maxCacheByteSize.fold(c5) { v => c5.withMaxCacheByteSize(v) }
    val c7  = dataFetchingStrategy.fold(c6) { v => c6.withDataFetchingStrategy(v) }
    val c8  = maxRecordsCount.fold(c7) { v => c7.withMaxRecordsCount(v) }
    val c9  = timeout.fold(c8) { v => c8.withTimeoutInSeconds(v.toSeconds.toInt); c8 }
    val c10 = idleMillisBetweenCalls.fold(c9) { v => c9.withIdleMillisBetweenCalls(v) }
    logWarningForTaskAfterMillis.fold(c10) { v => c10.withLogWarningForTaskAfterMillis(v.toMillis) }
  }

}
