package com.github.j5ik2o.sbt.aggregate.gen

case class TypeDesc(name: String)
case class ArgDesc(name: String, typeDesc: TypeDesc)

case class CommandDesc(name: String, parentTypeDesc: TypeDesc, argDescs: Seq[ArgDesc], replyDesc: Option[CommandReplyDesc])
case class CommandReplyDesc(name: String, parentTypeDesc: TypeDesc, argDescs: Seq[ArgDesc])

case class EventDesc(name: String, parentTypeDesc: TypeDesc, argDescs: Seq[ArgDesc])
