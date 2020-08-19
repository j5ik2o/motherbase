package com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.http.responder

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.http.json.CreateAccountResponseJson
import com.github.j5ik2o.motherbase.commandProcessor.CreateAccountResponse

final class CreateAccountJsonResponderImpl extends CreateAccountJsonResponder {
  override def response: Flow[CreateAccountResponse, CreateAccountResponseJson, NotUsed] = ???
}

import com.github.j5ik2o.motherbase.interfaceAdaptor.grpc.proto.{ CreateAccountResponse => GRPCCreateAccountResponse }

final class CreateAccountGrpcResponderImpl extends CreateAccountGrpcResponder {
  override def response: Flow[CreateAccountResponse, GRPCCreateAccountResponse, NotUsed] = ???
}
