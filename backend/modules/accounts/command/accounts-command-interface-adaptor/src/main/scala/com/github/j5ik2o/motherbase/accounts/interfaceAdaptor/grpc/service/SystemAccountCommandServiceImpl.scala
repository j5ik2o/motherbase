package com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.grpc.service

import com.github.j5ik2o.motherbase.interfaceAdaptor.grpc.proto._

import scala.concurrent.Future

class SystemAccountCommandServiceImpl extends SystemAccountCommandService {
  override def createSystemAccount(in: CreateSystemAccountRequest): Future[CreateSystemAccountResponse] = ???

  override def renameSystemAccount(in: RenameSystemAccountRequest): Future[RenameSystemAccountResponse] = ???
}
