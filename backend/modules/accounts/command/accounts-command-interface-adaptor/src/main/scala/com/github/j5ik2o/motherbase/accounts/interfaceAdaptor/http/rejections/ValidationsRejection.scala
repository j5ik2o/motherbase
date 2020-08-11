package com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.http.rejections

import cats.data.NonEmptyList
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.error.InterfaceError
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.http.reactions.MotherBaseRejection

case class ValidationsRejection(errors: NonEmptyList[InterfaceError]) extends MotherBaseRejection {
  override val message: String               = errors.toList.map(_.message).mkString(",")
  override val cause: Option[InterfaceError] = None
}
