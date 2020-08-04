package com.github.j5ik2o.sbt.aggregate.gen
import scala.meta._

object Main extends App {

  val program = """object Main extends App { print("Hello!") }"""
  val tree = program.parse[Source].get

  println(tree.syntax)

}
