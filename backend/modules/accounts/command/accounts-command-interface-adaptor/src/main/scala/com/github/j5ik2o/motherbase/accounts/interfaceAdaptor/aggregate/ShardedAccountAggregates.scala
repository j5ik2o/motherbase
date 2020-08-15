package com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.aggregate

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityContext, EntityTypeKey}
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.aggregate.AccountProtocol.{Idle, Stop}

import scala.concurrent.duration.FiniteDuration

object ShardedAccountAggregates {
  val name: String = "account"

  val TypeKey: EntityTypeKey[AccountProtocol.Command] = EntityTypeKey[AccountProtocol.Command]("Account")

  private def entityBehavior(
                              childBehavior: Behavior[AccountProtocol.Command],
                              actorName: String,
                              receiveTimeout: Option[FiniteDuration]
                            ): EntityContext[AccountProtocol.Command] => Behavior[AccountProtocol.Command] = { entityContext =>
    Behaviors.setup[AccountProtocol.Command] { ctx =>
      val childRef = ctx.spawn(childBehavior, actorName)
      receiveTimeout.foreach(ctx.setReceiveTimeout(_, Idle))
      Behaviors.receiveMessagePartial {
        case Idle =>
          entityContext.shard ! ClusterSharding.Passivate(ctx.self)
          Behaviors.same
        case Stop =>
          ctx.log.debug("> Changed state: Stop")
          Behaviors.stopped
        case msg =>
          childRef ! msg
          Behaviors.same
      }
    }
  }

  /**
   * ShardRegionへのプロキシー。
   *
   * @param clusterSharding [[ClusterSharding]]
   * @return 振る舞い
   */
  def ofProxy(clusterSharding: ClusterSharding): Behavior[AccountProtocol.Command] =
    Behaviors.receiveMessage[AccountProtocol.Command] { msg =>
      val entityRef = clusterSharding
        .entityRefFor[AccountProtocol.Command](TypeKey, msg.accountId.value.asString.reverse)
      entityRef ! msg
      Behaviors.same
    }

  /**
   * クラスターシャーディングを初期化し、ShardRegionへのActorRefを返す。
   *
   * @param clusterSharding [[ClusterSharding]]
   * @param childBehavior 子アクターの振るまい
   * @param receiveTimeout 受信タイムアウト
   * @return [[ActorRef]]
   */
  def initClusterSharding(
                           clusterSharding: ClusterSharding,
                           childBehavior: Behavior[AccountProtocol.Command],
                           receiveTimeout: Option[FiniteDuration]
                         ): ActorRef[ShardingEnvelope[AccountProtocol.Command]] = {
    val entity = Entity(TypeKey)(
      createBehavior = entityBehavior(
        childBehavior,
        AccountAggregates.name,
        receiveTimeout
      )
    )
    clusterSharding.init(
      receiveTimeout.fold(entity) { _ =>
        entity.withStopMessage(Stop)
      }
    )
  }

}
