resolvers ++= Seq(
  "Sonatype OSS Snapshot Repository" at "https://oss.sonatype.org/content/repositories/snapshots/",
  "Sonatype OSS Release Repository" at "https://oss.sonatype.org/content/repositories/releases/",
  "Seasar Repository" at "https://maven.seasar.org/maven2/",
  Resolver.bintrayRepo("beyondthelines", "maven"),
  Resolver.bintrayRepo("kamon-io", "sbt-plugins")
)

libraryDependencies ++= Seq(
  "com.h2database"    % "h2"                     % "1.4.195",
  "commons-io"        % "commons-io"             % "2.5",
  "org.seasar.util"   % "s2util"                 % "0.0.1",
  "com.github.j5ik2o" %% "reactive-aws-ecs-core" % "1.2.6",
  "com.thesamet.scalapb" %% "compilerplugin"        % "0.10.2"
)






























