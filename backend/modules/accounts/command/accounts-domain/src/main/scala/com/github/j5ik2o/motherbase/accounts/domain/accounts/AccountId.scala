package com.github.j5ik2o.motherbase.accounts.domain.accounts

import com.github.j5ik2o.motherbase.infrastructure.ulid.ULID

final case class AccountId private (value: ULID) {
  val modelName: String = getClass.getName.stripSuffix("Id")
}

object AccountId {
  def apply(value: ULID = ULID()): AccountId = new AccountId(value)
  def apply(value: String): AccountId        = apply(ULID.parseFromString(value).get)
}
