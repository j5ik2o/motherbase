package com.github.j5ik2o.motherbase.accounts.domain.system

import com.github.j5ik2o.motherbase.ulid.ULID

final case class SystemAccountId(value: ULID = ULID()) {
  val modelName: String = getClass.getName.stripSuffix("Id")
}
