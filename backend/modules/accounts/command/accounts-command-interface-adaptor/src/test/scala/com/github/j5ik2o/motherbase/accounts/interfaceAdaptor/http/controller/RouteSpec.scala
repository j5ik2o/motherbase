package com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.http.controller

import akka.actor.typed.scaladsl.adapter._
import akka.http.scaladsl.model.{ HttpEntity, MediaTypes }
import akka.http.scaladsl.testkit.{ RouteTestTimeout, ScalatestRouteTest }
import akka.testkit.TestKit
import akka.util.ByteString
import com.github.j5ik2o.motherbase.accounts.interfaceAdaptor.DISettings
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.Encoder
import io.circe.syntax._
import org.scalatest.matchers.should.Matchers
import org.scalatest.{ BeforeAndAfterAll, TestSuite }
import wvlet.airframe.{ Design, Session }

import scala.concurrent.duration._

trait RouteSpec extends ScalatestRouteTest with Matchers with BeforeAndAfterAll with FailFastCirceSupport {
  this: TestSuite =>

  implicit class ToHttpEntityOps[A: Encoder](json: A) {

    def toHttpEntity: HttpEntity.Strict = {
      val jsonAsByteString = ByteString(json.asJson.noSpaces)
      HttpEntity(MediaTypes.`application/json`, jsonAsByteString)
    }

  }

  def clusterMode: Boolean = false

  implicit def timeout: RouteTestTimeout = RouteTestTimeout(5 seconds)
  private var _session: Session          = _
  def session: Session                   = _session

  def design: Design =
    com.github.j5ik2o.motherbase.accounts.commandProcessor.DISettings.design
      .add(DISettings.designOfActorSystem(clusterMode, system.toTyped, materializer))
      .add(DISettings.designOfAggregates(clusterMode))
      .add(DISettings.designOfHttpControllers())
      .add(DISettings.designOfResponders())

  override def beforeAll(): Unit = {
    super.beforeAll()
    _session = design.newSession
    _session.start
  }

  override def afterAll(): Unit = {
    _session.shutdown
    TestKit.shutdownActorSystem(system)
    super.afterAll()
  }
}
