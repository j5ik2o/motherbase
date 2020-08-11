package com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.http.routes

import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.server.RouteResult
import akka.http.scaladsl.server.directives.LogEntry

trait RequestResultFormatter {
  def formatRequestResponse(request: HttpRequest): RouteResult => Option[LogEntry]
}
