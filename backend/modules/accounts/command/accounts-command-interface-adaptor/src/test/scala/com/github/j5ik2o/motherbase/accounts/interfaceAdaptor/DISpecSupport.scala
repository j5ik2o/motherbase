package com.github.j5ik2o.motherbase.accounts.interfaceAdaptor

import org.scalatest.{ BeforeAndAfterAll, TestSuite }
import wvlet.airframe.{ Design, Session }

trait DISpecSupport extends BeforeAndAfterAll { this: TestSuite =>
  private var _session: Session = _
  def session: Session          = _session

  def design: Design

  override def beforeAll(): Unit = {
    super.beforeAll()
    _session = design.newSession
    _session.start
  }

  override def afterAll(): Unit = {
    _session.shutdown
    super.afterAll()
  }
}
