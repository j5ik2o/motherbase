package com.github.j5ik2o.motherbase.accounts.domain.accounts

final case class AccountName(value: String) {
  require(value.nonEmpty)
  require(value.length <= 256)
}
