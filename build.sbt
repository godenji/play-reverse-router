import play.sbt.PlayScala

val scalaRelease = "2.13.3"
val scalaCrossVersions = Seq("2.12.12", scalaRelease)

lazy val root =
  project.in(file(".")).settings(
    name := "reverse-router",
    organization := "godenji",
    description := "generates reverse router",
    version := "0.1.5",
    scalaVersion := scalaRelease,
    crossScalaVersions := scalaCrossVersions,
    libraryDependencies +=
      "com.typesafe.play" %% "routes-compiler" % "2.8.2" withSources()
  ).
  enablePlugins(PlayScala)
