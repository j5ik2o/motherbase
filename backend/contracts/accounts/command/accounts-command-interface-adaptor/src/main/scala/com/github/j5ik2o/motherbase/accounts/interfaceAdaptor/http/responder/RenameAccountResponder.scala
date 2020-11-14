package com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.http.responder

import com.github.j5ik2o.motherbase.accounts.commandProcessor.RenameAccountResponse
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.http.json.RenameAccountResponseJson
import com.github.j5ik2o.motherbase.interfaceAdaptor.grpc.proto.{
  RenameAccountResponse => GRPCRenameAccountGprcResponse
}
trait RenameAccountJsonResponder extends Responder[RenameAccountResponse, RenameAccountResponseJson]

trait RenameAccountGRPCResponder extends Responder[RenameAccountResponse, GRPCRenameAccountGprcResponse]
