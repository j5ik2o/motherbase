package com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.http.reactions

import akka.http.javadsl.server.CustomRejection
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.error.InterfaceError

trait MotherBaseRejection extends CustomRejection {
  val message: String
  val cause: Option[InterfaceError]
  protected def withCauseMessage = s"$message${cause.fold("")(v => s": ${v.message}")}"
}
