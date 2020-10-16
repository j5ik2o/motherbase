package com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.readModelUpdater

import com.github.j5ik2o.motherbase.accounts.domain.accounts.AccountEvents.AccountCreated

import scala.concurrent.{ ExecutionContext, Future }

trait AccountEventWriter {
  def create(events: Seq[AccountCreated])(implicit ec: ExecutionContext): Future[Int]
  def create(event: AccountCreated)(implicit ec: ExecutionContext): Future[Unit]
  // def update(events: Seq[AccountRenamed])(implicit ec: ExecutionContext): Future[Int]
}
