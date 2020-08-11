package com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.http.routes

import akka.http.scaladsl.server.Directive0
import akka.http.scaladsl.server.directives.DebuggingDirectives

class RouteLogging()(implicit requestFormatter: RequestFormatter, requestResultFormatter: RequestResultFormatter) {

  val httpLogRequest: Directive0 = DebuggingDirectives.logRequest(requestFormatter.formatRequest _)

  val httpLogRequestResult: Directive0 =
    DebuggingDirectives.logRequestResult(requestResultFormatter.formatRequestResponse _)

}

object RouteLogging {

  def apply()(
      implicit requestFormatter: RequestFormatter,
      requestResultFormatter: RequestResultFormatter
  ): RouteLogging = new RouteLogging()

  val default: RouteLogging = new RouteLogging()(
    DefaultRequestLoggingFormatter.requestFormatter,
    DefaultRequestLoggingFormatter.requestResultFormatter
  )
}
