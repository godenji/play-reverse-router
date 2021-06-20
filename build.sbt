import play.sbt.PlayScala
import ReleaseTransformations._

val scalaRelease = "2.13.6"
val scalaCrossVersions = Seq("2.12.14", scalaRelease)

lazy val root =
  project.in(file(".")).settings(
    organization := "io.github.godenji",
    homepage := Some(url("https://github.com/godenji/play-reverse-router")),
    licenses := Seq("BSD 2-clause License" -> url(
      "https://github.com/godenji/play-reverse-router/blob/master/LICENSE.txt"
    )),
    name := "reverse-router",
    description := "generates global reverse router for one to many Play subproject(s)",
    scalaVersion := scalaRelease,
    crossScalaVersions := scalaCrossVersions,
    libraryDependencies += "com.typesafe.play" %% "routes-compiler" % "2.8.8" withSources(),
    releaseCrossBuild := true,
    releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      runClean,
      runTest,
      setReleaseVersion,
      commitReleaseVersion,
      tagRelease,
      releaseStepCommandAndRemaining("+publishSigned"),
      setNextVersion,
      commitNextVersion,
      releaseStepCommand("sonatypeReleaseAll"),
      pushChanges
    ),
    credentials ++= {
      val creds = Path.userHome / ".sonatype" / organization.value
      if (creds.exists) Seq(Credentials(creds)) else Nil
    },
    pomExtra := (
      <developers>
        <developer>
          <id>godenji</id>
          <name>N.S. Cutler</name>
          <url>https://github.com/godenji</url>
        </developer>
      </developers>
      <scm>
        <url>git@github.com:godenji/play-reverse-router.git</url>
        <connection>scm:git:git@github.com:godenji/play-reverse-router.git</connection>
      </scm>
    ),
    publishTo := sonatypePublishTo.value,
    scalacOptions in (Compile, doc) ++= {
      val hash = sys.process.Process("git rev-parse HEAD").lineStream_!.head
      Seq(
        "-sourcepath",
        (baseDirectory in LocalRootProject).value.getAbsolutePath,
        "-doc-source-url",
        s"https://github.com/godenji/play-reverse-router/tree/${hash}€{FILE_PATH}.scala"
      )
    }
  ).
  enablePlugins(PlayScala)
