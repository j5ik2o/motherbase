package com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.aggregate

import scala.concurrent.duration._

trait AggregateSpecScenarioBase {
  val testTimeFactor: Int         = sys.env.getOrElse("SBT_TEST_TIME_FACTOR", "1").toInt
  val maxDuration: FiniteDuration = (10 * testTimeFactor) seconds
}
