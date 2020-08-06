import Dependencies._
import com.amazonaws.regions.{Region, Regions}
import com.typesafe.sbt.SbtGit.git
import com.typesafe.sbt.SbtNativePackager.autoImport._
import com.typesafe.sbt.packager.archetypes.scripts.BashStartScriptPlugin.autoImport._
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport._
import com.typesafe.sbt.packager.docker.{Cmd, ExecCmd}
import org.scalafmt.sbt.ScalafmtPlugin.autoImport._
import org.scalastyle.sbt.ScalastylePlugin.autoImport._
import sbt.Keys._
import sbt.{Def, Resolver, _}
import sbtecr.EcrPlugin.autoImport._

object Settings {

  lazy val prefix: String = sys.env.get("PREFIX").fold("") { v => s"$v-" }

  lazy val projectBaseName = "motherbase"

  lazy val compileScalaStyle = taskKey[Unit]("compileScalaStyle")

  lazy val baseSettings =
    Seq(
      organization := "com.github.j5ik2o",
      name := projectBaseName,
      version := "1.0.0-SNAPSHOT",
      scalaVersion := scala213Version,
      scalacOptions ++=
        Seq(
          "-feature",
          "-deprecation",
          "-unchecked",
          "-encoding",
          "UTF-8",
          "-language:_",
          "-target:jvm-1.8"
        ),
      scalacOptions ++= {
        CrossVersion.partialVersion(scalaVersion.value) match {
          case Some((2, n)) if n >= 13 => "-Ymacro-annotations" :: Nil
          case _ => "-Ypartial-unification" :: Nil
        }
      },
//      wartremoverErrors in(Compile, compile) ++= Seq(
//        Wart.ArrayEquals,
//        Wart.AnyVal,
//        Wart.Var,
//        Wart.Null,
//        Wart.OptionPartial
//      ),
      resolvers ++= Seq(
        "Sonatype OSS Snapshot Repository" at "https://oss.sonatype.org/content/repositories/snapshots/",
        "Sonatype OSS Release Repository" at "https://oss.sonatype.org/content/repositories/releases/",
        "Akka Snapshots" at "https://repo.akka.io/snapshots",
        Resolver.bintrayRepo("akka", "snapshots"),
        "Seasar Repository" at "https://maven.seasar.org/maven2/",
        "DynamoDB Local Repository" at "https://s3-ap-northeast-1.amazonaws.com/dynamodb-local-tokyo/release",
        Resolver.bintrayRepo("beyondthelines", "maven"),
        Resolver.bintrayRepo("segence", "maven-oss-releases"),
        // Resolver.bintrayRepo("everpeace", "maven"),
        Resolver.bintrayRepo("tanukkii007", "maven"),
        Resolver.bintrayRepo("kamon-io", "snapshots")
      ),
      libraryDependencies ++= Seq(
        scalaLang.scalaReflect(scalaVersion.value),
        iheart.ficus,
        slf4j.api,
        sulky.ulid,
        monix.monix,
        timepit.refined,
        airframe.airframe,
        scalatest.scalatest % Test,
        scalacheck.scalacheck % Test,
        dimafeng.testcontainerScalaScalaTest % Test
      ),
      libraryDependencies ++= {
        CrossVersion.partialVersion(scalaVersion.value) match {
          case Some((2, n)) if n >= 13 => Nil
          case _ =>
            compilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full) :: Nil
        }
      },
//      dependencyOverrides ++= Seq(
//        "com.typesafe.akka" %% "akka-http" % "10.1.11",
//        "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.11"
//      ),
      scalafmtOnCompile := true,
      parallelExecution in Test := false,
      (scalastyleConfig in Compile) := file("scalastyle-config.xml"),
      compileScalaStyle := scalastyle.in(Compile).toTask("").value
    )

  lazy val ecrCommonSettings: Seq[Def.Setting[_]] = Seq(
    region in Ecr := Region.getRegion(Regions.AP_NORTHEAST_1),
    repositoryTags in Ecr ++= Seq((version in Docker).value),
    localDockerImage in Ecr := (packageName in Docker).value + ":" + (version in Docker).value,
    push in Ecr := ((push in Ecr) dependsOn(publishLocal in Docker, login in Ecr)).value
  )

  lazy val writeApiEcrSettings: Seq[Def.Setting[_]] = ecrCommonSettings ++ Seq(
    repositoryName in Ecr := s"$prefix$projectBaseName/$projectBaseName-write-api-server"
  )

  lazy val domainEventRouterEcrSettings: Seq[Def.Setting[_]] = ecrCommonSettings ++ Seq(
    repositoryName in Ecr := s"$prefix$projectBaseName/$projectBaseName-domain-event-router"
  )

  lazy val readModelUpdaterEcrSettings: Seq[Def.Setting[_]] = ecrCommonSettings ++ Seq(
    repositoryName in Ecr := s"$prefix$projectBaseName/$projectBaseName-read-model-updater"
  )

  lazy val readApiEcrSettings: Seq[Def.Setting[_]] = ecrCommonSettings ++ Seq(
    repositoryName in Ecr := s"$prefix$projectBaseName/$projectBaseName-read-api-server"
  )

  lazy val dockerCommonSettings = Seq(
    dockerBaseImage := "adoptopenjdk/openjdk8:x86_64-alpine-jdk8u242-b08",
    version in Docker := git.gitHeadCommit.value.get,
    maintainer in Docker := "Junichi Kato <j5ik2o@gmail.com>",
    dockerUpdateLatest := true,
    bashScriptExtraDefines ++= Seq(
      "addJava -Xms${JVM_HEAP_MIN:-1024m}",
      "addJava -Xmx${JVM_HEAP_MAX:-1024m}",
      "addJava -XX:MaxMetaspaceSize=${JVM_META_MAX:-512M}",
      "addJava ${JVM_GC_OPTIONS:--XX:+UseG1GC}",
      "addJava -Dconfig.resource=${CONFIG_RESOURCE:-application.conf}",
      "addJava -Dakka.remote.startup-timeout=60s"
    ),
    dockerCommands ++= Seq(
      Cmd("USER", "root"),
      // KamonでHostMetrics収集のための依存パッケージ (https://gitter.im/kamon-io/Kamon?at=5dce8551c26e8923c42bba9f)
      ExecCmd("RUN", "apk", "-v", "--update", "add", "udev", "curl"),
      // ユーザーをデフォルトユーザーに変更 (https://www.scala-sbt.org/sbt-native-packager/formats/docker.html#daemon-user)
      Cmd("USER", "demiourgos728")
    )
  )


}