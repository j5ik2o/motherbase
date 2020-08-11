package com.github.j5ik2o.motherbase.commandProcessor

import akka.NotUsed
import akka.stream.scaladsl.Flow

trait CommandProcessor[Req, Res] {
  def execute: Flow[Req, Res, NotUsed]
}
