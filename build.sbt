import play.sbt.PlayScala

lazy val root =
  project.in(file(".")).settings(
    name := "reverse-router",
    organization := "godenji",
    description := "generates reverse router",
    version := "0.1.5",
    scalaVersion := "2.13.3",
    libraryDependencies +=
      "com.typesafe.play" %% "routes-compiler" % "2.8.2" withSources()
  ).
  enablePlugins(PlayScala)
