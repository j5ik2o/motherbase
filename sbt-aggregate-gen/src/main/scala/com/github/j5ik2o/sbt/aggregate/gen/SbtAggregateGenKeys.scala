package com.github.j5ik2o.sbt.aggregate.gen

import sbt._

trait SbtAggregateGenKeys {
  val aggregateGen = taskKey[Unit]("sbt-aggregate-gen key")

  val templateDirectories = settingKey[Seq[File]]("template directories")
  val inputSourceDirectory = settingKey[File]("input source directory")
  val outputSourceDirectory = settingKey[File]("output source directory")

  val generateOne = inputKey[Seq[File]]("generate-one task")
  val generateMany = inputKey[Seq[File]]("generate-many task")
  val generateAll = taskKey[Seq[File]]("generate-all task")
}

object SbtAggregateGenKeys extends SbtAggregateGenKeys