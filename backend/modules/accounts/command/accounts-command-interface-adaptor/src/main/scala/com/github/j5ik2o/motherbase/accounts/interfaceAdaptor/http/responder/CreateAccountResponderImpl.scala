package com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.http.responder

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.http.json.CreateAccountResponseJson
import com.github.j5ik2o.motherbase.commandProcessor.CreateAccountResponse

final class CreateAccountResponderImpl extends CreateAccountResponder {
  override def response: Flow[CreateAccountResponse, CreateAccountResponseJson, NotUsed] = ???
}
