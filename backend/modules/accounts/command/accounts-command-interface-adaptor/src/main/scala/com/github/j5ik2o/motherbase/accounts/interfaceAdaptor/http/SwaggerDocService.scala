package com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.http

import com.github.swagger.akka.SwaggerHttpService
import com.github.swagger.akka.model.Info

class SwaggerDocService(hostName: String, port: Int, val apiClasses: Set[Class[_]]) extends SwaggerHttpService {
  override val host: String                     = s"127.0.0.1:$port" // the url of your api, not swagger's json endpoint
  override val apiDocsPath: String              = "api-docs" // where you want the swagger-json endpoint exposed
  override val info: Info                       = Info() // provides license and other description details
  override val unwantedDefinitions: Seq[String] = Seq("Function1", "Function1RequestContextFutureRouteResult")
}
