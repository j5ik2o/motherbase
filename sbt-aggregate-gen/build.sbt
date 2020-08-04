
lazy val buildSettings = Seq(
  organization := "com.github.j5ik2o",
  organizationName := "j5ik2o project",
  organizationHomepage := Some(new URL("https://github.com/j5ik2o")),
  publishArtifact in Test := false,
  sbtPlugin := true,
  parallelExecution := true,
  scalacOptions ++= Seq("-encoding", "UTF-8", "-deprecation", "-unchecked"),
  scriptedBufferLog := false,
  scriptedLaunchOpts := {
    scriptedLaunchOpts.value ++ Seq("-Xmx1024M", "-XX:MaxPermSize=256M", "-Dplugin.version=" + version.value)
  },
  crossSbtVersions := Vector("1.2.7"),
  //  releaseCrossBuild := true,
  //  releaseTagName := { (version in ThisBuild).value },
  //  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  //  releaseProcess := Seq[ReleaseStep](
  //    checkSnapshotDependencies,
  //    inquireVersions,
  //    runClean,
  //    releaseStepCommandAndRemaining("^ test"),
  //    setReleaseVersion,
  //    commitReleaseVersion,
  //    tagRelease,
  //    releaseStepCommandAndRemaining("^ publishSigned"),
  //    setNextVersion,
  //    commitNextVersion,
  //    releaseStepCommand("sonatypeReleaseAll"),
  //    pushChanges
  //  ),
  credentials += Credentials((baseDirectory in LocalRootProject).value / ".credentials")
)

// Project modules
lazy val sbtSonatype = Project(
  id = "sbt-aggregate-gen",
  base = file(".")
).enablePlugins(ScriptedPlugin)
  .settings(buildSettings)
  .settings(
    resolvers ++= Seq(
      "Sonatype OSS Snapshot Repository" at "https://oss.sonatype.org/content/repositories/snapshots/",
      "Sonatype OSS Release Repository" at "https://oss.sonatype.org/content/repositories/releases/",
      "Typesafe Releases" at "https://repo.typesafe.com/typesafe/releases/",
      "Seasar Repository" at "https://maven.seasar.org/maven2/"
    ),
    libraryDependencies ++= Seq(
      "ch.qos.logback" % "logback-classic" % "1.1.3",
      "org.slf4j" % "slf4j-api" % "1.7.12",
      "org.scalameta" %% "scalameta" % "4.3.20",
      "com.lihaoyi" %% "scalaparse" % "2.0.5",
      "com.iheart" %% "ficus" % "1.4.3",
      "org.seasar.util" % "s2util" % "0.0.1",
      "com.beachape" %% "enumeratum" % "1.5.13",
      "org.scala-lang.modules" %% "scala-java8-compat" % "0.9.0",
      "org.scalatest" %% "scalatest" % "3.0.1" % Test,
      "com.h2database" % "h2" % "1.4.187" % Test
    )
  )