package com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.http.json

final case class RenameAccountResponseJson(account_id: String, errors: Option[ErrorsResponseJson]) extends ResponseJson
