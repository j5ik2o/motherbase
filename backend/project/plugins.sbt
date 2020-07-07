resolvers ++= Seq(
  "Sonatype OSS Snapshot Repository" at "https://oss.sonatype.org/content/repositories/snapshots/",
  "Sonatype OSS Release Repository" at "https://oss.sonatype.org/content/repositories/releases/",
  "Seasar Repository" at "https://maven.seasar.org/maven2/",
  Resolver.bintrayRepo("kamon-io", "sbt-plugins")
)

libraryDependencies ++= Seq(
  "com.h2database"    % "h2"                     % "1.4.195",
  "commons-io"        % "commons-io"             % "2.5",
  "org.seasar.util"   % "s2util"                 % "0.0.1",
  "com.github.j5ik2o" %% "reactive-aws-ecs-core" % "1.1.3"
)

addSbtPlugin("io.kamon" % "sbt-aspectj-runner" % "1.1.0")

addSbtPlugin("com.lightbend.sbt" % "sbt-javaagent" % "0.1.4")

addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "1.0.0")

addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.0.0")

addSbtPlugin("org.wartremover" % "sbt-wartremover" % "2.4.9")

addSbtPlugin("com.typesafe.sbt" % "sbt-multi-jvm" % "0.4.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.3.10")

addSbtPlugin("io.gatling" % "gatling-sbt" % "2.2.2")

addSbtPlugin("io.spray" % "sbt-revolver" % "0.9.1")

addSbtPlugin("org.lyranthe.sbt" % "partial-unification" % "1.1.2")

addSbtPlugin("io.github.davidmweber" % "flyway-sbt" % "5.2.0")

addSbtPlugin("jp.co.septeni-original" % "sbt-dao-generator" % "1.0.8")

addSbtPlugin("com.chatwork" % "sbt-wix-embedded-mysql" % "1.0.9")

addSbtPlugin("com.lightbend.akka.grpc" % "sbt-akka-grpc" % "0.6.1")

addSbtPlugin("com.mintbeans" % "sbt-ecr" % "0.14.1")

addSbtPlugin("org.ensime" % "sbt-ensime" % "2.5.1")