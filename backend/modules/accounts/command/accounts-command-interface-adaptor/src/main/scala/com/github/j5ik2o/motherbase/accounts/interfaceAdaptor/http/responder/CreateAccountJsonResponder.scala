package com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.http.responder

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.http.json.{
  CreateAccountResponseJson,
  ErrorsResponseJson
}
import com.github.j5ik2o.motherbase.accounts.commandProcessor.CreateAccountResponse
import com.github.j5ik2o.motherbase.accounts.domain.accounts.AccountId
import com.github.j5ik2o.motherbase.interfaceAdaptor.grpc.proto.{
  Error,
  CreateAccountResponse => GRPCCreateAccountResponse
}

final class CreateAccountJsonResponderImpl extends CreateAccountJsonResponder {

  override def response: Flow[CreateAccountResponse, CreateAccountResponseJson, NotUsed] =
    Flow[CreateAccountResponse].map { res =>
      CreateAccountResponseJson(
        res.accountId.value.asString,
        res.error.map(v => ErrorsResponseJson(Seq(v.message)))
      )
    }
}

final class CreateAccountGRPCResponderImpl extends CreateAccountGRPCResponder {

  override def response: Flow[CreateAccountResponse, GRPCCreateAccountResponse, NotUsed] =
    Flow[CreateAccountResponse].map { response =>
      GRPCCreateAccountResponse(
        accountId = response.accountId.value.asString,
        errors = response.error.map { e => Seq(Error(e.message)) }.getOrElse(Seq.empty[Error])
      )
    }
}
