package com.github.j5ik2o.sbt.aggregate.gen

import sbt.{AutoPlugin, PluginTrigger, Plugins}
import sbt.plugins.JvmPlugin

object SbtAggregateGen extends AutoPlugin {

  override def trigger: PluginTrigger = allRequirements

  override def requires: Plugins = JvmPlugin

}
