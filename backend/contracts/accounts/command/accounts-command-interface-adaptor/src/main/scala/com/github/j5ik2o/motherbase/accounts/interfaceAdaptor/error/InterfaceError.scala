package com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.error

sealed trait InterfaceError {
  val message: String
  val cause: Option[Throwable]
}

case class SystemAccountIdFormatError(message: String, cause: Option[Throwable] = None) extends InterfaceError
case class SystemAccountNameError(message: String, cause: Option[Throwable] = None)     extends InterfaceError
case class EmailAddressError(message: String, cause: Option[Throwable] = None)          extends InterfaceError
