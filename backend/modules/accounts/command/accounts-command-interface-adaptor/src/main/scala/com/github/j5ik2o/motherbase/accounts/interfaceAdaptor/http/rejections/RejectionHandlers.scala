package com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.http.rejections

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.complete
import akka.http.scaladsl.server.RejectionHandler
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.http.json.ErrorsResponseJson
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._

object RejectionHandlers {

  final val default: RejectionHandler = RejectionHandler
    .newBuilder()
    .handle {
      case NotFoundRejection(msg, _) =>
        complete((StatusCodes.NotFound, ErrorsResponseJson(Seq(msg))))
      case ValidationsRejection(errors) =>
        complete((StatusCodes.BadRequest, ErrorsResponseJson(errors.map(_.message).toList)))
    }
    .result()

}
