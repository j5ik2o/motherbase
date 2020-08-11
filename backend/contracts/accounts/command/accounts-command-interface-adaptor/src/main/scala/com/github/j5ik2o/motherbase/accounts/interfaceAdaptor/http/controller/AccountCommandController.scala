package com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.http.controller

import akka.http.scaladsl.server.Route
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.http.json.{
  CreateAccountRequestJson,
  CreateAccountResponseJson
}
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.{ Content, Schema }
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import javax.ws.rs._

@Path("/v1")
@Produces(Array("application/json"))
trait AccountCommandController {
  def toRoutes: Route

  @POST
  @Path("accounts/create")
  @Consumes(Array("application/json"))
  @Operation(
    summary = "Create account",
    description = "Create account request",
    requestBody = new RequestBody(
      content = Array(
        new Content(
          schema = new Schema(implementation = classOf[CreateAccountRequestJson])
        )
      )
    ),
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Create account response",
        content = Array(new Content(schema = new Schema(implementation = classOf[CreateAccountResponseJson])))
      ),
      new ApiResponse(responseCode = "500", description = "Internal server error")
    )
  )
  private[controller] def createAccount: Route
}
