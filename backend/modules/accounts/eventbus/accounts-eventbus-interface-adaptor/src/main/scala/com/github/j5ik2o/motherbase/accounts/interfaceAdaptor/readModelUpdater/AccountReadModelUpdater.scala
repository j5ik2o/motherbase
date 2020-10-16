package com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.readModelUpdater

import akka.{ Done, NotUsed }
import akka.actor.ActorSystem
import akka.actor.typed.{ ActorRef, Behavior }
import akka.actor.typed.scaladsl.{ ActorContext, Behaviors }
import akka.kafka.ConsumerMessage.CommittableOffset
import akka.kafka.scaladsl.{ Committer, Consumer }
import akka.kafka.scaladsl.Consumer.DrainingControl
import akka.kafka.{ CommitterSettings, ConsumerMessage, ConsumerSettings, Subscriptions }
import akka.persistence.PersistentRepr
import akka.serialization.SerializationExtension
import akka.stream.{ ActorAttributes, Attributes, Supervision }
import akka.stream.scaladsl.{ Flow, Keep, RestartSource, Sink, Source }
import com.typesafe.config.Config
import kamon.Kamon
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.{ ByteArrayDeserializer, StringDeserializer }
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal
import scala.util.{ Failure, Success }
import scala.concurrent.duration._

object AccountReadModelUpdater {
  sealed trait Command
  sealed trait CommandReply
  final case class Start(replyTo: Option[ActorRef[StartReply]] = None)      extends Command
  final case class StartReply()                                             extends CommandReply
  final case class Stop(replyTo: Option[ActorRef[StopReply]] = None)        extends Command
  final case class StopReply()                                              extends CommandReply
  final case class Restart()                                                extends Command
  private case class DrainAndShutdown(replyTo: Option[ActorRef[StopReply]]) extends Command
}

final class AccountReadModelUpdater(config: Config, accountEventWriter: AccountEventWriter)(
    implicit system: ActorSystem
) extends ReadModelUpdaterSupport {
  import com.github.j5ik2o.motherbase.accounts.domain.accounts.AccountEvents._
  import AccountReadModelUpdater._
  val projectionFlowCounter     = Kamon.counter("projection-flow-count").withTag("event-type", "account")
  val deserializeMessageCounter = Kamon.counter("deserialize-message-count").withTag("event-type", "account")
  val dynamodbWriteCounter      = Kamon.counter("dynamodb-write-count").withTag("event-type", "account")

  private val logger = LoggerFactory.getLogger(getClass)

  private val topic: String  = config.getString("topic")
  private val consumerConfig = config.getConfig("consumer")

  private val consumerSettings: ConsumerSettings[String, Array[Byte]] =
    ConsumerSettings(consumerConfig, new StringDeserializer, new ByteArrayDeserializer)

  private val committerSettings: CommitterSettings = CommitterSettings(config.getConfig("committer-settings"))
  private val parallelism: Int                     = config.getInt("parallelism")
  private var control: DrainingControl[Done]       = _

  private val serialization = SerializationExtension(system)

  type Message = ConsumerMessage.CommittableMessage[String, Array[Byte]]

  def behavior: Behavior[Command] = stopping

  private def projectionFlow(
      topicPartition: TopicPartition
  )(
      implicit ec: ExecutionContext
  ): Flow[Message, CommittableOffset, NotUsed] = {
    logger.debug(s"topicPartition = $topicPartition")
    Flow[ConsumerMessage.CommittableMessage[String, Array[Byte]]]
      .mapAsync(1) { message =>
        def generateSpan(opName: String) = Kamon.spanBuilder(opName).tag("event-type", "account")
        val projectionFlowSpan           = generateSpan("projection-flow").start()
        projectionFlowCounter.increment()

        deserializeMessageCounter.increment()
        logger.debug(s"message = $message")
        val persistentRepr = serialization.deserialize(message.record.value(), classOf[PersistentRepr]) match {
          case Success(value) =>
            logger.debug(s"success: ${message.committableOffset}")
            value
          case Failure(ex) =>
            logger.error(s"occurred error: ${message.committableOffset}", ex)
            throw ex
        }
        dynamodbWriteCounter.increment()
        val span  = generateSpan("dynamo-db-write").asChildOf(projectionFlowSpan).start()
        val event = persistentRepr.payload.asInstanceOf[AccountEvent]
        logger.debug(s"manifest = ${persistentRepr.manifest}")

        RestartSource // FIXME: config に切り出す
          .withBackoff(
            minBackoff = 100.millis,
            maxBackoff = 30.second,
            randomFactor = 0.2,
            maxRestarts = Int.MaxValue // いったんリバランス回避目的 & 問題の単純化のため Int.MaxValue
          ) { () =>
            val future = event match {
              case e: AccountCreated => accountEventWriter.create(e)
//              case e: AccountRenamed => accountEventWriter.update(e)
            }
            future.onComplete {
              case Success(_) =>
                logger.info(s"success: event = $event, committableOffset = ${message.committableOffset}")
                span.finish()
                projectionFlowSpan.finish()
              case Failure(ex) =>
                logger.error(s"occurred error: event = $event, committableOffset = ${message.committableOffset}", ex)
                span.fail(ex).finish()
                projectionFlowSpan.finish()
            }
            Source.future(future.map(_ => message.committableOffset))
          }.runWith(
            Sink.head
          ) // runWith せず Source のまま Flow.flatMapConcat に渡すと RestartSource の挙動がおかしくなる(エラーがなくても再実行し続ける)
      }
  }

  private def running: Behavior[Command] = Behaviors.setup[Command] { ctx =>
    import ctx.executionContext
    ctx.log.info(s"topic = $topic")
    Behaviors
      .receiveMessagePartial[Command] {
        case Stop(replyToOpt) =>
          ctx.pipeToSelf(control.drainAndShutdown()) {
            case Success(_)  => DrainAndShutdown(replyToOpt)
            case Failure(ex) => throw ex
          }
          Behaviors.same
        case DrainAndShutdown(replyToOpt) =>
          replyToOpt.foreach(_ ! StopReply())
          Behaviors.stopped
        case Restart() => // FIXME: 期待通りの Restart はまだ出来ていない
          ctx.log.info("Restarting ...")
          ctx.pipeToSelf(control.stop()) {
            case Success(_) =>
              ctx.log.info("control.drainAndShutdown() succeeded.")
              Start()
            case Failure(ex) => throw ex
          }
          stopping //Behaviors.stopped { () => ctx.log.info("PostStop log in Restart") }
      }
      .receiveSignal {
        case (_, signal) =>
          ctx.log.info(s"[running] receiveSignal: $signal")
          Behaviors.same
      }
  }

  private def stopping: Behavior[Command] = Behaviors.setup[Command] { ctx =>
    import ctx.executionContext
    ctx.log.info(s"topic = $topic")
    Behaviors
      .receiveMessagePartial[Command] {
        case Start(replyToOpt) =>
          val partitionCounts = retrievePartitionCounts(topic, consumerSettings, 30.seconds)
          ctx.log.info(s"topic(`$topic`)'s partition counts = $partitionCounts")
          control = Consumer
            .committablePartitionedSource(
              consumerSettings,
              Subscriptions.topics(topic)
            )
            .log("kafka source")
            .mapAsyncUnordered(partitionCounts) {
              case (topicPartition, partitionedSource) =>
                partitionedSource
                  .via(projectionFlow(topicPartition))
                  .runWith(Committer.sink(committerSettings))
            }
            .addAttributes(
              Attributes.logLevels(
                onElement = Attributes.LogLevels.Info,
                onFinish = Attributes.LogLevels.Info,
                onFailure = Attributes.LogLevels.Error
              )
            )
            .toMat(Sink.ignore)(Keep.both)
            .mapMaterializedValue(DrainingControl(_))
            .withAttributes(ActorAttributes.supervisionStrategy(decider(ctx)))
            .run()
          control.streamCompletion.onComplete {
            case Success(v) =>
              ctx.log.info(s"[streamCompletion] $v")
            case Failure(ex) =>
              ctx.log.error("[streamCompletion] occurred error", ex)
          }

          ctx.system.scheduler.scheduleAtFixedRate(10 second, 3 second) { () =>
            control.metrics.foreach { map => ctx.log.info(s"metrics: ${map.mkString("\n")}") }
          }

          replyToOpt.foreach(_ ! StartReply())
          running
      }
      .receiveSignal {
        case (_, signal) =>
          ctx.log.info(s"receiveSignal: $signal")
          Behaviors.same
      }
  }

  private def decider(ctx: ActorContext[Command]): Supervision.Decider = {
    case NonFatal(e) =>
      logger.error("Stream will be stopped", e)
      ctx.self ! Restart()
      Supervision.Stop // Supervision.Restart は要素が Skip されるので使わないこと. see: https://doc.akka.io/docs/akka/current/stream/stream-error.html
  }
}
