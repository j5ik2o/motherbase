package com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.grpc.service

import com.github.j5ik2o.motherbase.interfaceAdaptor.grpc.proto._

import scala.concurrent.Future

class AccountCommandServiceImpl extends AccountCommandService {
  override def createSystemAccount(in: CreateAccountRequest): Future[CreateAccountResponse] = ???

  override def renameSystemAccount(in: RenameAccountRequest): Future[RenameAccountResponse] = ???

  override def destroySystemAccount(in: CreateAccountRequest): Future[CreateAccountResponse] = ???
}
