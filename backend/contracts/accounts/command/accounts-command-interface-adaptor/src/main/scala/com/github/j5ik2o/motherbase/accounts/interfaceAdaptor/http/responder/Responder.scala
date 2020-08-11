package com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.http.responder

import akka.NotUsed
import akka.stream.scaladsl.Flow

trait Responder[Res, ResRepr] {
  def response: Flow[Res, ResRepr, NotUsed]
}
