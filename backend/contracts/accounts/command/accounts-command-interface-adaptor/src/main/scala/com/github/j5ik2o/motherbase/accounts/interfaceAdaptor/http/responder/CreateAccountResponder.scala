package com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.http.responder

import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.http.json.CreateAccountResponseJson
import com.github.j5ik2o.motherbase.commandProcessor.CreateAccountResponse

trait CreateAccountResponder extends Responder[CreateAccountResponse, CreateAccountResponseJson]
