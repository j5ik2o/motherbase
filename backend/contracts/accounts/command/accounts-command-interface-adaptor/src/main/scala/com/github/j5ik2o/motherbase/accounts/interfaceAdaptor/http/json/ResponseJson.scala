package com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.http.json

trait ResponseJson {
  def errors: Option[ErrorsResponseJson]
  def isSuccessful: Boolean = errors.fold(true)(_.error_messages.isEmpty)
}
