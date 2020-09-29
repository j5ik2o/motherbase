package com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.router

import akka.actor.typed.scaladsl.ActorContext
import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.{ IRecordProcessor, IRecordProcessorFactory }
import com.typesafe.config.Config

final class AccountEventProcessorFactory(ctx: ActorContext[_], config: Config) extends IRecordProcessorFactory {
  override def createProcessor(): IRecordProcessor = new AccountEventProcessor(ctx, config)
}
