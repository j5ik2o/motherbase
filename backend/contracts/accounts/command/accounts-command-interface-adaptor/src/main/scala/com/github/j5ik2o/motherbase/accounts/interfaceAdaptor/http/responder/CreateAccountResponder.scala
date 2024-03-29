package com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.http.responder

import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.http.json.CreateAccountResponseJson
import com.github.j5ik2o.motherbase.accounts.commandProcessor.CreateAccountResponse
import com.github.j5ik2o.motherbase.interfaceAdaptor.grpc.proto.{
  CreateAccountResponse => GRPCCreateAccountGprcResponse
}

trait CreateAccountJsonResponder extends Responder[CreateAccountResponse, CreateAccountResponseJson]

trait CreateAccountGRPCResponder extends Responder[CreateAccountResponse, GRPCCreateAccountGprcResponse]
