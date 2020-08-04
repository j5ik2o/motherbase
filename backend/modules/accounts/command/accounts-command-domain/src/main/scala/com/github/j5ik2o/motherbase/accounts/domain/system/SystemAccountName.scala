package com.github.j5ik2o.motherbase.accounts.domain.system

final case class SystemAccountName(value: String) {
  require(value.nonEmpty)
  require(value.length <= 256)
}
