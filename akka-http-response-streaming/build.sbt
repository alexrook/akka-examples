lazy val rootMergeStrategy = Seq(assemblyMergeStrategy in assembly := {
  case m if m.toLowerCase.endsWith("manifest.mf")     => MergeStrategy.discard
  case m if m.toLowerCase.matches("meta-inf.*\\.sf$") => MergeStrategy.discard
  case "log4j.properties"                             => MergeStrategy.first
  case m if m.toLowerCase.startsWith("meta-inf/services/") =>
    MergeStrategy.filterDistinctLines
  case m if m.toLowerCase.contains("license") => MergeStrategy.concat
  case "reference.conf"                       => MergeStrategy.concat
  case _                                      => MergeStrategy.first
})

lazy val root = (project in file("."))
  .settings(
    inThisBuild(List(organization := "com.example", scalaVersion := "2.12.4")),
    name := "akka-http-response-streaming",
    libraryDependencies ++= Seq(
      "com.typesafe.akka"  %% "akka-http"                 % "10.1.3",
      "com.typesafe.akka"  %% "akka-stream"               % "2.5.12",
      "com.typesafe.akka"  %% "akka-http-spray-json"      % "10.1.3",
      "io.spray"           %% "spray-json"                % "1.3.4",
      "com.lightbend.akka" %% "akka-stream-alpakka-hbase" % "1.1.1",
//      "org.apache.hadoop" % "hadoop-common" % "2.7.1" excludeAll ExclusionRule(
//        organization = "javax.servlet"),
      "org.apache.hbase" % "hbase-common" % "1.1.2",
      "org.apache.hbase" % "hbase-client" % "1.1.2"
    ),
    mainClass := Some("com.example.Main"),
    resolvers ++= Seq("Dathena" at "http://repository.dathena.io/repository/dathena-main")
  )
  .settings(rootMergeStrategy)
