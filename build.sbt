import ApplicationBuild._

lazy val root =
  project.in(file(".")).settings(
    name := appName,
    organization := "godenji",
    description := "generates reverse router",
    version := appVersion,
    scalaVersion := scalaRelease,
    libraryDependencies +=
      "com.typesafe.play" %% "routes-compiler" % playVersion withSources()
  ).
  enablePlugins(play.sbt.PlayScala)