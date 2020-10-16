package com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.readModelUpdater

import org.apache.kafka.common.KafkaFuture

import scala.concurrent.{ Future, Promise }

trait EnrichKafkaFuture {

  implicit class EnrichKafkaFuture[A](val self: KafkaFuture[A]) {

    def asScala: Future[A] = {
      val promise = Promise[A]()
      self.whenComplete {
        case (_, throwable: Throwable) if throwable != null => promise.failure(throwable)
        case (v, _)                                         => promise.success(v)
      }
      promise.future
    }

  }
}

object EnrichKafkaFuture extends EnrichKafkaFuture
