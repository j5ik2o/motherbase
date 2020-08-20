package com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.http.responder

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.http.json.{ CreateAccountResponseJson, ErrorJson }
import com.github.j5ik2o.motherbase.commandProcessor.CreateAccountResponse
import com.github.j5ik2o.motherbase.interfaceAdaptor.grpc.proto.{ CreateAccountResponse => GRPCCreateAccountResponse }

final class CreateAccountJsonResponderImpl extends CreateAccountJsonResponder {

  override def response: Flow[CreateAccountResponse, CreateAccountResponseJson, NotUsed] =
    Flow[CreateAccountResponse].map { res =>
      CreateAccountResponseJson(
        res.accountId.value.asString,
        res.error.map(v => ErrorJson(v.message))
      )
    }
}

final class CreateAccountGRPCResponderImpl extends CreateAccountGRPCResponder {
  override def response: Flow[CreateAccountResponse, GRPCCreateAccountResponse, NotUsed] = ???
}
