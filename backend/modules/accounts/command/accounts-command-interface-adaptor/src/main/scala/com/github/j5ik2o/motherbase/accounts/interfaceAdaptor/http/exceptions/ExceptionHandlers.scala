package com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.http.exceptions

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.complete
import akka.http.scaladsl.server.ExceptionHandler
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.http.json.ErrorsResponseJson
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._

object ExceptionHandlers {

  final val default = ExceptionHandler {
    case ex: Exception =>
      complete((StatusCodes.InternalServerError, ErrorsResponseJson(Seq(ex.getMessage))))
  }

}
