lazy val root = (project in file("."))
  .settings(
    name := "project"
  )
version := "0.1"

scalaVersion := "2.13.15"

libraryDependencies ++= Seq(
  "biz.enef" %% "slogging-slf4j" % "0.6.2",
  "org.slf4j" % "slf4j-simple" % "2.0.13",
  "com.bot4s" %% "telegram-core" % "5.8.3",
  "com.bot4s" %% "telegram-akka" % "5.8.3",
  "com.softwaremill.sttp.client3" %% "core" % "3.9.7",
  "com.softwaremill.sttp.client3" %% "async-http-client-backend-future" % "3.9.6",
  "com.github.pureconfig" %% "pureconfig" % "0.17.7",
  "com.typesafe" % "config" % "1.4.3"

)
