val catsV = "1.1.0"
val catsEffectV = "0.10.1"
val log4sV = "1.6.1"
val specs2V = "4.0.4"

lazy val log4cats = project.in(file("."))
  .aggregate(
    coreJVM,
    coreJS,
    testingJVM,
    testingJS,
    log4s,
    scribeJVM,
    scribeJS,
    slf4j
  )
  .settings(noPublishSettings)
  .settings(commonSettings, releaseSettings)

lazy val core = crossProject.in(file("core"))
  .settings(commonSettings, releaseSettings)
  .settings(
    name := "log4cats-core"
  )

lazy val coreJVM = core.jvm
lazy val coreJS  = core.js

lazy val testing = crossProject.in(file("testing"))
  .settings(commonSettings, releaseSettings)
  .dependsOn(core)
  .settings(
    name := "log4cats-testing"
  )

lazy val testingJVM = testing.jvm
lazy val testingJS = testing.js

lazy val log4s = project.in(file("log4s"))
  .settings(commonSettings, releaseSettings)
  .dependsOn(core.jvm)
  .settings(
    name := "log4cats-log4s",
    libraryDependencies ++= Seq(
      "org.log4s"                   %% "log4s"                      % log4sV,
    )
  )

lazy val slf4j = project.in(file("slf4j"))
  .settings(commonSettings, releaseSettings)
  .dependsOn(core.jvm)
  .settings(
    name := "log4cats-slf4j",
    libraryDependencies ++= Seq(
      "org.slf4j" % "slf4j-api" % "1.7.25"
    )
  )

lazy val scribe = crossProject.in(file("scribe"))
  .settings(commonSettings, releaseSettings)
  .dependsOn(core)
  .settings(
    name := "log4cats-scribe",
    libraryDependencies ++= Seq(
      "com.outr" %%% "scribe" % "2.3.3"
    )
  )

lazy val scribeJVM = scribe.jvm
lazy val scribeJS = scribe.js

lazy val contributors = Seq(
  "ChristopherDavenport" -> "Christopher Davenport"
)

lazy val commonSettings = Seq(
  organization := "io.chrisdavenport",

  scalaVersion := "2.12.4",
  crossScalaVersions := Seq(scalaVersion.value, "2.11.12"),

  addCompilerPlugin("org.spire-math" % "kind-projector" % "0.9.6" cross CrossVersion.binary),

  libraryDependencies ++= Seq(
    "org.typelevel"               %%% "cats-core"                  % catsV,
    "org.typelevel"               %%% "cats-effect"                % catsEffectV,

    "org.specs2"                  %%% "specs2-core"                % specs2V       % Test
    // "org.specs2"                  %% "specs2-scalacheck"          % specs2V       % Test
  )
)

lazy val releaseSettings = {
  import ReleaseTransformations._
  Seq(
    releaseCrossBuild := true,
    releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      runClean,
      runTest,
      setReleaseVersion,
      commitReleaseVersion,
      tagRelease,
      // For non cross-build projects, use releaseStepCommand("publishSigned")
      releaseStepCommandAndRemaining("+publishSigned"),
      setNextVersion,
      commitNextVersion,
      releaseStepCommand("sonatypeReleaseAll"),
      pushChanges
    ),
    publishTo := {
      val nexus = "https://oss.sonatype.org/"
      if (isSnapshot.value)
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases" at nexus + "service/local/staging/deploy/maven2")
    },
    credentials ++= (
      for {
        username <- Option(System.getenv().get("SONATYPE_USERNAME"))
        password <- Option(System.getenv().get("SONATYPE_PASSWORD"))
      } yield
        Credentials(
          "Sonatype Nexus Repository Manager",
          "oss.sonatype.org",
          username,
          password
        )
    ).toSeq,
    publishArtifact in Test := false,
    releasePublishArtifactsAction := PgpKeys.publishSigned.value,
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/ChristopherDavenport/log4cats"),
        "git@github.com:ChristopherDavenport/log4cats.git"
      )
    ),
    homepage := Some(url("https://github.com/ChristopherDavenport/log4cats")),
    licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
    publishMavenStyle := true,
    pomIncludeRepository := { _ =>
      false
    },
    pomExtra := {
      <developers>
        {for ((username, name) <- contributors) yield
        <developer>
          <id>{username}</id>
          <name>{name}</name>
          <url>http://github.com/{username}</url>
        </developer>
        }
      </developers>
    }
  )
}

lazy val noPublishSettings = {
  import com.typesafe.sbt.pgp.PgpKeys.publishSigned
  Seq(
    publish := {},
    publishLocal := {},
    publishSigned := {},
    publishArtifact := false
  )
}
