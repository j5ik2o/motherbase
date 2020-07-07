import com.amazonaws.regions.{Region, Regions}
import com.typesafe.sbt.SbtNativePackager.autoImport._
import com.typesafe.sbt.packager.archetypes.scripts.BashStartScriptPlugin.autoImport._
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport._
import org.scalafmt.sbt.ScalafmtPlugin.autoImport._
import org.scalastyle.sbt.ScalastylePlugin.autoImport._
import sbt.Keys._
import sbt._
import sbtecr.EcrPlugin.autoImport._
import wartremover.WartRemover.autoImport._

object Settings {
  val akkaVersion           = "2.6.6"
  val akkaHttpVersion       = "10.1.8"
  val akkaManagementVersion = "1.0.8"
  val circeVersion          = "0.11.1"
  val monocleVersion        = "1.5.0"
  val swaggerVersion        = "2.0.8"
  val slickVersion          = "3.2.3"
  val gatlingVersion        = "3.1.2"

  val compileScalaStyle = taskKey[Unit]("compileScalaStyle")

  val ecrSettings = Seq(
    region in Ecr := Region.getRegion(Regions.AP_NORTHEAST_1),
    repositoryName in Ecr := "j5ik2o/motherbase",
    repositoryTags in Ecr ++= Seq(version.value),
    localDockerImage in Ecr := "j5ik2o/" + (packageName in Docker).value + ":" + (version in Docker).value,
    push in Ecr := ((push in Ecr) dependsOn (publishLocal in Docker, login in Ecr)).value
  )

  lazy val dockerCommonSettings = Seq(
    dockerBaseImage := "adoptopenjdk/openjdk8:x86_64-alpine-jdk8u191-b12",
    maintainer in Docker := "Junichi Kato <j5ik2o@gmail.com>",
    dockerUpdateLatest := true,
    bashScriptExtraDefines ++= Seq(
      "addJava -Xms${JVM_HEAP_MIN:-1024m}",
      "addJava -Xmx${JVM_HEAP_MAX:-1024m}",
      "addJava -XX:MaxMetaspaceSize=${JVM_META_MAX:-512M}",
      "addJava ${JVM_GC_OPTIONS:--XX:+UseG1GC}",
      "addJava -Dconfig.resource=${CONFIG_RESOURCE:-application.conf}",
      "addJava -Dakka.remote.startup-timeout=60s"
    )
  )

  val baseSettings = Seq(
    scalaVersion := "2.13.3",
    version := "1.0.0-SNAPSHOT",
    scalacOptions ++= Seq(
      "-feature",
      "-deprecation",
      "-unchecked",
      "-encoding",
      "UTF-8",
      "-Xfatal-warnings",
      "-language:_",
      // Warn if an argument list is modified to match the receiver
      "-Ywarn-adapted-args",
      // Warn when dead code is identified.
      "-Ywarn-dead-code",
      // Warn about inaccessible types in method signatures.
      "-Ywarn-inaccessible",
      // Warn when a type argument is inferred to be `Any`.
      "-Ywarn-infer-any",
      // Warn when non-nullary `def f()' overrides nullary `def f'
      "-Ywarn-nullary-override",
      // Warn when nullary methods return Unit.
      "-Ywarn-nullary-unit",
      // Warn when numerics are widened.
      "-Ywarn-numeric-widen",
      // Warn when imports are unused.
      "-Ywarn-unused-import"
    ),
    scalafmtOnCompile in ThisBuild := true,
    wartremoverErrors in (Compile, compile) ++= Seq(
      Wart.ArrayEquals,
      Wart.AnyVal,
      Wart.Var,
      Wart.Null,
      Wart.OptionPartial
    ),
    resolvers ++= Seq(
      "Sonatype OSS Snapshot Repository" at "https://oss.sonatype.org/content/repositories/snapshots/",
      "Sonatype OSS Release Repository" at "https://oss.sonatype.org/content/repositories/releases/",
      "DynamoDB Local Repository" at "https://s3-ap-northeast-1.amazonaws.com/dynamodb-local-tokyo/release",
      "Seasar Repository" at "https://maven.seasar.org/maven2/",
      Resolver.bintrayRepo("segence", "maven-oss-releases"),
      Resolver.bintrayRepo("everpeace", "maven"),
      Resolver.bintrayRepo("tanukkii007", "maven"),
      Resolver.bintrayRepo("kamon-io", "snapshots")
    ),
//    libraryDependencies ++= Seq(
//      "io.monix"           %% "monix"          % "3.0.0-RC2",
//      "eu.timepit"         %% "refined"        % "0.9.5",
//      "org.wvlet.airframe" %% "airframe"       % "19.4.1",
//      "org.scalatest"      %% "scalatest"      % "3.0.4" % Test,
//      "ch.qos.logback"     % "logback-classic" % "1.2.3" % Test
//    ),
    parallelExecution in Test := false,
    fork := true,
    (scalastyleConfig in Compile) := file("scalastyle-config.xml"),
    compileScalaStyle := scalastyle.in(Compile).toTask("").value,
    (compile in Compile) := (compile in Compile).dependsOn(compileScalaStyle).value
  )
}