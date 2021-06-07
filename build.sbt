import Dependencies._

ThisBuild / scalaVersion     := "2.13.5"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.example"
ThisBuild / organizationName := "example"

val catsVersion = "2.3.0"
val zioVersion = "1.0.9"

lazy val root = (project in file("."))
  .settings(
    name := "SPod",
    libraryDependencies ++= Seq(
      scalaTest % Test,
      "com.softwaremill.sttp.client3" %% "httpclient-backend-zio" % "3.3.6",
      "org.scala-lang.modules" %% "scala-xml" % "2.0.0",
      "org.typelevel" %% "cats-core" % catsVersion,
      "dev.zio" %% "zio" % zioVersion
    )
  )
