import Dependencies._
import GatlingSettings._
import Settings._
import com.typesafe.sbt.SbtNativePackager.autoImport._
import com.typesafe.sbt.packager.archetypes.scripts.BashStartScriptPlugin.autoImport._
import com.typesafe.sbt.packager.docker.Cmd
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport._
import sbt.Keys._
import sbt._

val othersDir = "others"

val `healthchecks-core` = project
  .in(file(s"$othersDir/healthchecks/core"))
  .settings(baseSettings)

val `healthchecks-k8s-probes` = project
  .in(file(s"$othersDir/healthchecks/k8s-probes"))
  .settings(baseSettings)
  .dependsOn(`healthchecks-core` % "test->test;compile->compile")

// --- modules

val `common-infrastructure` =
  (project in file("modules/common/common-infrastructure"))
    .settings(baseSettings)
    .settings(
      name := s"$projectBaseName-common-infrastructure",
      libraryDependencies ++= Seq(
          logback.classic % Test,
          beachape.enumeratum,
          airframe.airframeControl
        )
    )

val `accounts-common-infrastructure` =
  (project in file("modules/accounts/common/accounts-common-infrastructure"))
    .settings(baseSettings)
    .settings(
      name := s"$projectBaseName-accounts-common-infrastructure",
      libraryDependencies ++= Seq(
          logback.classic % Test,
          beachape.enumeratum
        )
    ).dependsOn(`common-infrastructure`)

val `accounts-domain` = (project in file("modules/accounts/command/accounts-domain"))
  .settings(baseSettings)
  .settings(
    name := s"$projectBaseName-accounts-command-domain",
    jigModelPattern in jig := ".+\\.domain\\.(model|type)\\.[^$]+",
    jigReports in jig := ((jigReports in jig).dependsOn(compile in Compile)).value
  )
  .dependsOn(`accounts-common-infrastructure`)

// --- contracts

val swaggerLibraryDependencies = Seq(
  "javax.ws.rs"                  % "javax.ws.rs-api"            % "2.0.1",
  "com.github.swagger-akka-http" %% "swagger-akka-http"         % "2.1.1",
  "com.github.swagger-akka-http" %% "swagger-scala-module"      % "2.1.2",
  "com.github.swagger-akka-http" %% "swagger-enumeratum-module" % "2.0.0",
  "com.fasterxml.jackson.module" %% "jackson-module-scala"      % Version.jackson,
  "pl.iterators"                 %% "kebs-akka-http"            % "1.7.1",
  "io.swagger.core.v3"           % "swagger-core"               % Version.swagger,
  "io.swagger.core.v3"           % "swagger-annotations"        % Version.swagger,
  "io.swagger.core.v3"           % "swagger-models"             % Version.swagger,
  "io.swagger.core.v3"           % "swagger-jaxrs2"             % Version.swagger,
  "ch.megard"                    %% "akka-http-cors"            % "0.4.3"
)

val `accounts-command-processor-contracts` =
  (project in file("contracts/accounts/command/accounts-command-processor"))
    .settings(baseSettings)
    .settings(
      name := s"$projectBaseName-accounts-command-processor-contracts",
      libraryDependencies ++= Seq(
          akka.actorTyped,
          akka.stream
        )
    )
    .dependsOn(`accounts-domain`)

val `accounts-command-interface-adaptor-contracts` =
  (project in file("contracts/accounts/command/accounts-command-interface-adaptor"))
    .enablePlugins(AkkaGrpcPlugin)
    .settings(baseSettings)
    .settings(
      name := s"$projectBaseName-accounts-command-interface-contracts",
      libraryDependencies ++= swaggerLibraryDependencies,
      libraryDependencies ++= Seq(
          akka.actorTyped,
          akka.slf4j,
          akka.stream,
          "com.thesamet.scalapb.common-protos" %% "proto-google-common-protos-scalapb_0.10" % "1.17.0-0" % "protobuf",
          "com.thesamet.scalapb.common-protos" %% "proto-google-common-protos-scalapb_0.10" % "1.17.0-0"
        ),
      PB.protoSources in Compile += target.value / "protobuf_external",
      PB.protoSources in Compile += (baseDirectory in LocalRootProject).value / "protobuf" / "command"
    ).dependsOn(`accounts-domain`, `accounts-command-processor-contracts`)

val `accounts-query-interface-adaptor-contracts` =
  (project in file("contracts/accounts/query/accounts-query-interface-adaptor"))
    .enablePlugins(AkkaGrpcPlugin)
    .settings(baseSettings)
    .settings(
      name := s"$projectBaseName-accounts-query-interface-adaptor-contracts",
      libraryDependencies ++= swaggerLibraryDependencies,
      libraryDependencies ++= Seq(
          akka.actorTyped,
          akka.slf4j,
          akka.stream,
          "com.thesamet.scalapb.common-protos" %% "proto-google-common-protos-scalapb_0.10" % "1.17.0-0" % "protobuf",
          "com.thesamet.scalapb.common-protos" %% "proto-google-common-protos-scalapb_0.10" % "1.17.0-0"
        ),
      PB.protoSources in Compile += target.value / "protobuf_external",
      PB.protoSources in Compile += (baseDirectory in LocalRootProject).value / "protobuf" / "query"
    )

val `contract-query-processor` =
  (project in file("contracts/accounts/query/accounts-query-processor"))
    .settings(baseSettings)
    .settings(
      name := s"$projectBaseName-accounts-query-processor-contracts",
      libraryDependencies ++= Seq(
          )
    )

// --- modules

val `accounts-command-processor` =
  (project in file("modules/accounts/command/accounts-command-processor"))
    .settings(baseSettings)
    .settings(
      name := s"$projectBaseName-accounts-command-processor",
      libraryDependencies ++= Seq(
          akka.actorTyped,
          logback.classic    % Test,
          akka.testKitTyped  % Test,
          akka.streamTestKit % Test,
          akka.httpTestKit   % Test
        )
    )
    .dependsOn(
      `accounts-command-processor-contracts`,
      `accounts-command-interface-adaptor-contracts`,
      `accounts-common-infrastructure`,
      `accounts-domain`
    )

val `accounts-query-processor` =
  (project in file("modules/accounts/query/accounts-query-processor"))
    .settings(baseSettings)
    .settings(
      name := s"$projectBaseName-accounts-query-processor",
      libraryDependencies ++= Seq(
          akka.actorTyped,
          logback.classic    % Test,
          akka.testKitTyped  % Test,
          akka.streamTestKit % Test
        )
    )
    .dependsOn(
      `contract-query-processor`,
      `accounts-query-interface-adaptor-contracts`,
      `accounts-common-infrastructure`
    )

val `interface-adaptor-common` = (project in file("modules/common/common-interface-adaptor"))
  .settings(baseSettings)
  .settings(
    name := s"$projectBaseName-common-interface-adaptor",
    libraryDependencies ++= Seq(
//      akka.actorTyped,
//      akka.discovery,
//      j5ik2o.reactiveAwsDynamodb,
//      j5ik2o.reactiveAwsS3,
//      circe.core,
//      circe.generic,
//      circe.parser,
//      akka.http,
//      kamon.bundle,
//      kamon.datadog,
//      kamon.status,
//      heikoseeberger.akkaHttpCirce,
//      akka.testKitTyped % Test
      )
  )
  .dependsOn(`healthchecks-k8s-probes`)

val `accounts-interface-adaptor-common` = (project in file("modules/accounts/common/accounts-common-interface-adaptor"))
  .settings(baseSettings)
  .settings(
    name := s"$projectBaseName-accounts-common-interface-adaptor",
    libraryDependencies ++= Seq(
        akka.actorTyped,
        akka.discovery,
        "com.amazonaws" % "dynamodb-streams-kinesis-adapter" % "1.5.1",
        aws.v1.dynamodb,
        j5ik2o.reactiveAwsDynamodb,
        j5ik2o.reactiveAwsS3,
        circe.core,
        circe.generic,
        circe.parser,
        akka.http,
        kamon.bundle,
        kamon.datadog,
        kamon.status,
        heikoseeberger.akkaHttpCirce,
        akka.testKitTyped                     % Test,
        dimafeng.testcontainerScalaKafka      % Test,
        dimafeng.testcontainerScalaLocalstack % Test,
        embeddedkafka.embeddedKafka           % Test
//        testcontainers.testcontainers           % Test,
//        testcontainers.testcontainersLocalStack % Test,
//        testcontainers.testcontainersKafka      % Test
      )
  )
  .dependsOn(`healthchecks-k8s-probes`, `interface-adaptor-common`)

val `accounts-interface-adaptor-query` =
  (project in file("modules/accounts/query/accounts-query-interface-adaptor"))
    .settings(baseSettings)
    .settings(
      name := s"$projectBaseName-accounts-query-interface-adaptor",
      libraryDependencies ++= Seq(
          )
    )
    .dependsOn(
      `accounts-query-interface-adaptor-contracts`,
      `accounts-interface-adaptor-common`,
      `accounts-common-infrastructure`
    )

val `accounts-interface-adaptor-command` =
  (project in file("modules/accounts/command/accounts-command-interface-adaptor"))
    .settings(baseSettings)
    .settings(
      name := s"$projectBaseName-accounts-command-interface-adaptor",
      libraryDependencies ++= Seq(
          aws.v1.sts,
          akka.clusterTyped,
          akka.streamKafka,
          akka.streamKafkaClusterSharding,
          akka.discovery,
          akka.clusterShardingTyped,
          akka.persistenceTyped,
          akka.serializationJackson,
          megard.akkaHttpCors,
          j5ik2o.akkaPersistenceDynamodb,
          j5ik2o.akkaPersistenceS3,
          j5ik2o.akkaKinesisKcl,
          akkaManagement.akkaManagement,
          akkaManagement.clusterHttp,
          akkaManagement.clusterBootstrap,
          akkaManagement.k8sApi,
          aspectj.aspectjweaver,
          logback.classic             % Test,
          akka.httpTestKit            % Test,
          akka.testKit                % Test,
          akka.testKitTyped           % Test,
          akka.streamTestKit          % Test,
          akka.multiNodeTestKit       % Test,
          embeddedkafka.embeddedKafka % Test,
          slf4j.julToSlf4j            % Test
        )
    )
    .dependsOn(
      `accounts-command-interface-adaptor-contracts`,
      `accounts-interface-adaptor-common` % "compile->compile;test->test",
      `accounts-common-infrastructure`,
      `accounts-command-processor`
    )

val `accounts-interface-adaptor-eventbus` =
  (project in file("modules/accounts/eventbus/accounts-eventbus-interface-adaptor"))
    .settings(baseSettings)
    .settings(
      name := s"$projectBaseName-accounts-eventbus-interface-adaptor",
      libraryDependencies ++= Seq(
          aws.v1.sts,
          akka.clusterTyped,
          akka.streamKafka,
          akka.streamKafkaClusterSharding,
          akka.discovery,
          akka.clusterShardingTyped,
          akka.persistenceTyped,
          akka.serializationJackson,
          megard.akkaHttpCors,
          j5ik2o.akkaPersistenceDynamodb,
          j5ik2o.akkaPersistenceS3,
          j5ik2o.akkaKinesisKcl,
          akkaManagement.akkaManagement,
          akkaManagement.clusterHttp,
          akkaManagement.clusterBootstrap,
          akkaManagement.k8sApi,
          aspectj.aspectjweaver,
          logback.classic             % Test,
          akka.httpTestKit            % Test,
          akka.testKit                % Test,
          akka.testKitTyped           % Test,
          akka.streamTestKit          % Test,
          akka.multiNodeTestKit       % Test,
          embeddedkafka.embeddedKafka % Test,
          slf4j.julToSlf4j            % Test
        )
    )
    .dependsOn(
      `accounts-command-interface-adaptor-contracts`,
      `accounts-interface-adaptor-common`  % "compile->compile;test->test",
      `accounts-interface-adaptor-command` % "test->test",
      `accounts-common-infrastructure`,
      `accounts-command-processor`
    )
// ---- bootstrap

val `write-grpc-server` = (project in file("bootstrap/write-grpc-server"))
  .enablePlugins(AshScriptPlugin, JavaAgent, EcrPlugin)
  .settings(baseSettings)
  .settings(dockerCommonSettings)
  .settings(writeApiEcrSettings)
  .settings(
    name := s"$projectBaseName-write-grpc-server",
    mainClass in (Compile, run) := Some(organization.value + s".$projectBaseName.grpc.server.Main"),
    mainClass in reStart := Some(organization.value + s".$projectBaseName.grpc.server.Main"),
    dockerEntrypoint := Seq(s"/opt/docker/bin/$projectBaseName-write-grpc-server"),
    dockerExposedPorts := Seq(2222, 2223),
    packageName in Docker := s"$projectBaseName/${name.value}",
    fork in run := true,
    javaAgents += "io.kamon" % "kanela-agent" % "1.0.5",
    javaOptions in Universal += "-Dorg.aspectj.tracing.factory=default",
    javaOptions in run ++= Seq(
        s"-Dcom.sun.management.jmxremote.port=${sys.env.getOrElse("JMX_PORT", "8999")}",
        "-Dcom.sun.management.jmxremote.authenticate=false",
        "-Dcom.sun.management.jmxremote.ssl=false",
        "-Dcom.sun.management.jmxremote.local.only=false",
        "-Dcom.sun.management.jmxremote"
      ),
    javaOptions in Universal ++= Seq(
        "-Dcom.sun.management.jmxremote",
        "-Dcom.sun.management.jmxremote.local.only=true",
        "-Dcom.sun.management.jmxremote.authenticate=false"
      ),
    libraryDependencies ++= Seq(
        scopt.scopt,
        logback.logstashLogbackEncoder,
        slf4j.julToSlf4j,
        logback.classic,
        jaino.jaino,
        aws.v1.sts
      )
  )
  .dependsOn(`accounts-interface-adaptor-command`, `accounts-common-infrastructure`)

val `read-grpc-server` = (project in file("bootstrap/read-grpc-server"))
  .enablePlugins(AshScriptPlugin, JavaAgent, EcrPlugin)
  .settings(baseSettings)
  .settings(dockerCommonSettings)
  .settings(readApiEcrSettings)
  .settings(
    name := s"$projectBaseName-read-api-server",
    mainClass in (Compile, run) := Some(organization.value + s".$projectBaseName.grpc.server.Main"),
    mainClass in reStart := Some(organization.value + s".$projectBaseName.grpc.server.Main"),
    dockerEntrypoint := Seq(s"/opt/docker/bin/$projectBaseName-read-grpc-server"),
    packageName in Docker := s"$projectBaseName/${name.value}",
    fork in run := true,
    javaAgents += "io.kamon" % "kanela-agent" % "1.0.5",
    javaOptions in Universal += "-Dorg.aspectj.tracing.factory=default",
    javaOptions in run ++= Seq(
        s"-Dcom.sun.management.jmxremote.port=${sys.env.getOrElse("JMX_PORT", "8999")}",
        "-Dcom.sun.management.jmxremote.authenticate=false",
        "-Dcom.sun.management.jmxremote.ssl=false",
        "-Dcom.sun.management.jmxremote.local.only=false",
        "-Dcom.sun.management.jmxremote"
      ),
    javaOptions in Universal ++= Seq(
        "-Dcom.sun.management.jmxremote",
        "-Dcom.sun.management.jmxremote.local.only=true",
        "-Dcom.sun.management.jmxremote.authenticate=false"
      ),
    libraryDependencies ++= Seq(
        scopt.scopt,
        logback.logstashLogbackEncoder,
        slf4j.julToSlf4j,
        logback.classic,
        jaino.jaino,
        aws.v1.sts
      )
  )
  .dependsOn(`accounts-interface-adaptor-query`, `accounts-common-infrastructure`)

val `read-model-updater` = (project in file("bootstrap/read-model-updater"))
  .enablePlugins(AshScriptPlugin, JavaAgent, EcrPlugin)
  .settings(baseSettings)
  .settings(dockerCommonSettings)
  .settings(readModelUpdaterEcrSettings)
  .settings(
    name := s"$projectBaseName-read-model-updater",
    mainClass in (Compile, run) := Some(organization.value + s".$projectBaseName.rmu.Main"),
    mainClass in reStart := Some(organization.value + s".$projectBaseName.rmu.Main"),
    dockerEntrypoint := Seq(s"/opt/docker/bin/$projectBaseName-read-model-updater"),
    packageName in Docker := s"$projectBaseName/${name.value}",
    fork in run := true,
    javaAgents += "io.kamon" % "kanela-agent" % "1.0.5",
    javaOptions in Universal += "-Dorg.aspectj.tracing.factory=default",
    javaOptions in run ++= Seq(
        s"-Dcom.sun.management.jmxremote.port=${sys.env.getOrElse("JMX_PORT", "8999")}",
        "-Dcom.sun.management.jmxremote.authenticate=false",
        "-Dcom.sun.management.jmxremote.ssl=false",
        "-Dcom.sun.management.jmxremote.local.only=false",
        "-Dcom.sun.management.jmxremote"
      ),
    javaOptions in Universal ++= Seq(
        "-Dcom.sun.management.jmxremote",
        "-Dcom.sun.management.jmxremote.local.only=true",
        "-Dcom.sun.management.jmxremote.authenticate=false"
      ),
    libraryDependencies ++= Seq(
        scopt.scopt,
        logback.logstashLogbackEncoder,
        slf4j.julToSlf4j,
        logback.classic,
        jaino.jaino,
        aws.v1.sts
      )
  )
  .dependsOn(`accounts-interface-adaptor-command`, `accounts-common-infrastructure`)

val `domain-event-router` = (project in file("bootstrap/domain-event-router"))
  .enablePlugins(AshScriptPlugin, JavaAgent, EcrPlugin)
  .settings(baseSettings)
  .settings(dockerCommonSettings)
  .settings(domainEventRouterEcrSettings)
  .settings(
    name := s"$projectBaseName-domain-event-router",
    mainClass in (Compile, run) := Some(organization.value + s".$projectBaseName.der.Main"),
    mainClass in reStart := Some(organization.value + s".$projectBaseName.der.Main"),
    dockerEntrypoint := Seq(s"/opt/docker/bin/$projectBaseName-domain-event-router"),
    packageName in Docker := s"$projectBaseName/${name.value}",
    fork in run := true,
    javaAgents += "io.kamon" % "kanela-agent" % "1.0.5",
    javaOptions in Universal += "-Dorg.aspectj.tracing.factory=default",
    javaOptions in run ++= Seq(
        s"-Dcom.sun.management.jmxremote.port=${sys.env.getOrElse("JMX_PORT", "8999")}",
        "-Dcom.sun.management.jmxremote.authenticate=false",
        "-Dcom.sun.management.jmxremote.ssl=false",
        "-Dcom.sun.management.jmxremote.local.only=false",
        "-Dcom.sun.management.jmxremote"
      ),
    javaOptions in Universal ++= Seq(
        "-Dcom.sun.management.jmxremote",
        "-Dcom.sun.management.jmxremote.local.only=true",
        "-Dcom.sun.management.jmxremote.authenticate=false"
      ),
    libraryDependencies ++= Seq(
        scopt.scopt,
        logback.logstashLogbackEncoder,
        slf4j.julToSlf4j,
        logback.classic,
        jaino.jaino
      )
  )
  .dependsOn(`accounts-interface-adaptor-command`, `accounts-common-infrastructure`)

val `test-grpc-client` = (project in file("bootstrap/test-grpc-client"))
  .enablePlugins(AshScriptPlugin, JavaAgent)
  .settings(baseSettings)
  .settings(dockerCommonSettings)
  .settings(
    name := s"$projectBaseName-test-grpc-client",
    mainClass in (Compile, run) := Some(organization.value + s".$projectBaseName.grpc.client.Main"),
    mainClass in reStart := Some(organization.value + s".$projectBaseName.grpc.client.Main"),
    dockerEntrypoint := Seq(s"/opt/docker/bin/$projectBaseName-test-grpc-client"),
    packageName in Docker := s"$projectBaseName/${name.value}",
    fork in run := true,
    javaAgents += "io.kamon" % "kanela-agent" % "1.0.5",
    javaOptions in Universal += "-Dorg.aspectj.tracing.factory=default",
    javaOptions in run ++= Seq(
        s"-Dcom.sun.management.jmxremote.port=${sys.env.getOrElse("JMX_PORT", "8999")}",
        "-Dcom.sun.management.jmxremote.authenticate=false",
        "-Dcom.sun.management.jmxremote.ssl=false",
        "-Dcom.sun.management.jmxremote.local.only=false",
        "-Dcom.sun.management.jmxremote"
      ),
    javaOptions in Universal ++= Seq(
        "-Dcom.sun.management.jmxremote",
        "-Dcom.sun.management.jmxremote.local.only=true",
        "-Dcom.sun.management.jmxremote.authenticate=false"
      ),
    libraryDependencies ++= Seq(
        scopt.scopt,
        logback.logstashLogbackEncoder,
        slf4j.julToSlf4j,
        logback.classic,
        jaino.jaino
      )
  )
  .dependsOn(
    `accounts-command-interface-adaptor-contracts`,
    `accounts-query-interface-adaptor-contracts`,
    `accounts-common-infrastructure`
  )

// --- gatling

lazy val `gatling-test` = (project in file("tools/aws-gatling-tools/gatling-test"))
  .enablePlugins(GatlingPlugin, ProtocPlugin)
  .settings(gatlingBaseSettings)
  .settings(
    name := "gatling-test",
    libraryDependencies += ("io.grpc" % "protoc-gen-grpc-java" % "1.23.0") asProtocPlugin (),
    libraryDependencies ++= Seq(
        "io.gatling.highcharts" % "gatling-charts-highcharts" % gatlingVersion,
        "io.gatling"            % "gatling-test-framework"    % gatlingVersion,
        "com.amazonaws"         % "aws-java-sdk-core"         % awsSdkVersion,
        "com.amazonaws"         % "aws-java-sdk-s3"           % awsSdkVersion,
        "io.circe"              %% "circe-core"               % circeVersion,
        "io.circe"              %% "circe-generic"            % circeVersion,
        "io.circe"              %% "circe-parser"             % circeVersion,
        "io.grpc"               % "grpc-netty"                % scalapb.compiler.Version.grpcJavaVersion,
        "com.thesamet.scalapb"  %% "scalapb-runtime-grpc"     % scalapb.compiler.Version.scalapbVersion,
        "com.github.phisgr"     %% "gatling-grpc"             % "0.8.2",
        sulky.ulid
      ),
    publishArtifact in (GatlingIt, packageBin) := true,
    PB.targets in Compile := Seq(
        PB.gens.java                        -> (sourceManaged in Compile).value,
        scalapb.gen(javaConversions = true) -> (sourceManaged in Compile).value
      ),
    PB.protoSources in Compile ++= Seq(
        (baseDirectory in LocalRootProject).value / "protobuf" / "command",
        (baseDirectory in LocalRootProject).value / "protobuf" / "query"
      )
  )
  .settings(
    addArtifact(artifact in (GatlingIt, packageBin), packageBin in GatlingIt)
  )

lazy val `gatling-runner` = (project in file("tools/aws-gatling-tools/gatling-runner"))
  .enablePlugins(JavaAppPackaging, EcrPlugin)
  .settings(gatlingBaseSettings)
  .settings(gatlingRunnerEcrSettings)
  .settings(
    name := "gatling-runner",
    libraryDependencies ++= Seq(
        "io.gatling"    % "gatling-app"       % gatlingVersion,
        "com.amazonaws" % "aws-java-sdk-core" % awsSdkVersion,
        "com.amazonaws" % "aws-java-sdk-s3"   % awsSdkVersion
      ),
    mainClass in (Compile, bashScriptDefines) := Some(
        "com.github.j5ik2o.gatling.runner.Runner"
      ),
    dockerBaseImage := "openjdk:8",
    packageName in Docker := s"$projectBaseName/gatling-runner",
    dockerUpdateLatest := true,
    dockerCommands ++= Seq(
        Cmd("USER", "root"),
        Cmd("RUN", "mkdir /var/log/gatling"),
        Cmd("RUN", "chown daemon:daemon /var/log/gatling"),
        Cmd("ENV", "GATLING_RESULT_DIR=/var/log/gatling")
      )
  )
  .dependsOn(`gatling-test` % "compile->gatling-it")

lazy val buildDockerImage = taskKey[Unit]("Execute maven clean scripts")
import scala.sys.process._

lazy val `gatling-s3-reporter` = (project in file("tools/aws-gatling-tools/gatling-s3-reporter"))
  .settings(
    name := "gatling-s3-reporter",
    buildDockerImage := {
      val s: TaskStreams = streams.value
      s.log.info("make build ...")
      if (((Seq("bash", "-c") :+ "cd tools/aws-gatling-tools/gatling-s3-reporter && make build") !) == 0) {
        s.log.success("make build successful!")
      } else {
        throw new IllegalStateException("frontend maven clean failed!")
      }
    },
    compile in Compile := ((compile in Compile).dependsOn(buildDockerImage)).value
  )

lazy val `gatling-aggregate-runner` =
  (project in file("tools/aws-gatling-tools/gatling-aggregate-runner"))
    .enablePlugins(JavaAppPackaging, EcrPlugin)
    .settings(gatlingBaseSettings)
    .settings(gatlingAggregateRunnerEcrSettings)
    .settings(gatlingAggregateRunTaskSettings)
    .settings(
      name := "gatling-aggregate-runner",
      mainClass in (Compile, bashScriptDefines) := Some(
          "com.github.j5ik2o.gatling.runner.Runner"
        ),
      dockerBaseImage := "openjdk:8",
      packageName in Docker := s"$projectBaseName/gatling-aggregate-runner",
      dockerUpdateLatest := true,
      libraryDependencies ++= Seq(
          "org.slf4j"           % "slf4j-api"              % "1.7.26",
          "ch.qos.logback"      % "logback-classic"        % "1.2.3",
          "org.codehaus.janino" % "janino"                 % "3.0.6",
          "com.iheart"          %% "ficus"                 % "1.4.6",
          "com.github.j5ik2o"   %% "reactive-aws-ecs-core" % "1.1.3",
          "org.scalaj"          %% "scalaj-http"           % "2.4.2"
        )
    )

val root = (project in file("."))
  .settings(baseSettings)
  .settings(name := s"$projectBaseName-root")
  .aggregate(
    `write-grpc-server`,
    `domain-event-router`,
    `read-model-updater`,
    `read-grpc-server`,
    `test-grpc-client`,
    `common-infrastructure`,
    `accounts-common-infrastructure`,
    `accounts-interface-adaptor-common`,
    `accounts-interface-adaptor-command`,
    `accounts-interface-adaptor-query`,
    `accounts-command-processor`,
    `accounts-domain`,
    `accounts-query-processor`,
    `gatling-test`,
    `gatling-runner`,
    `gatling-s3-reporter`,
    `gatling-aggregate-runner`
  )
