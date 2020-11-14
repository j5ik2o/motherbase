package com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.http.responder

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.github.j5ik2o.motherbase.accounts.commandProcessor.RenameAccountResponse
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.http.json.{
  ErrorsResponseJson,
  RenameAccountResponseJson
}
import com.github.j5ik2o.motherbase.interfaceAdaptor.grpc.proto.{
  Error,
  RenameAccountResponse => GRPCRenameAccountResponse
}

final class RenameAccountJsonResponderImpl extends RenameAccountJsonResponder {

  override def response: Flow[RenameAccountResponse, RenameAccountResponseJson, NotUsed] =
    Flow[RenameAccountResponse].map { res =>
      RenameAccountResponseJson(
        res.accountId.value.asString,
        res.error.map(v => ErrorsResponseJson(Seq(v.message)))
      )
    }
}

final class RenameAccountGRPCResponderImpl extends RenameAccountGRPCResponder {

  override def response: Flow[RenameAccountResponse, GRPCRenameAccountResponse, NotUsed] =
    Flow[RenameAccountResponse].map { response =>
      GRPCRenameAccountResponse(
        accountId = response.accountId.value.asString,
        errors = response.error.map { e => Seq(Error(e.message)) }.getOrElse(Seq.empty[Error])
      )
    }
}
