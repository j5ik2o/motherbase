package com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.http.rejections

import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.error.InterfaceError
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.http.reactions.MotherBaseRejection

case class NotFoundRejection(override val message: String, override val cause: Option[InterfaceError])
    extends MotherBaseRejection
