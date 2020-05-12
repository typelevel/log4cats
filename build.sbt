import sbtcrossproject.{crossProject, CrossType}
val catsV = "2.1.1"
val catsEffectV = "2.1.3"
val slf4jV = "1.7.30"
val specs2V = "4.9.4"
val logbackClassicV = "1.2.3"

lazy val log4cats = project
  .in(file("."))
  .aggregate(
    coreJVM,
    coreJS,
    testingJVM,
    testingJS,
    noopJVM,
    noopJS,
    slf4j,
    docs
  )
  .settings(noPublishSettings)
  .settings(commonSettings, releaseSettings)

lazy val docs = project
  .settings(noPublishSettings)
  .settings(commonSettings, micrositeSettings)
  .enablePlugins(MicrositesPlugin)
  .dependsOn(slf4j)

lazy val core = crossProject(JSPlatform, JVMPlatform)
  .settings(commonSettings, releaseSettings, mimaSettings)
  .settings(
    name := "log4cats-core",
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "cats-core" % catsV
    )
  )
lazy val coreJVM = core.jvm
lazy val coreJS = core.js

lazy val testing = crossProject(JSPlatform, JVMPlatform)
  .settings(commonSettings, releaseSettings, mimaSettings)
  .dependsOn(core)
  .settings(
    name := "log4cats-testing",
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "cats-effect" % catsEffectV
    )
  )
lazy val testingJVM = testing.jvm
lazy val testingJS = testing.js

lazy val noop = crossProject(JSPlatform, JVMPlatform)
  .settings(commonSettings, mimaSettings, releaseSettings)
  .dependsOn(core)
  .settings(
    name := "log4cats-noop"
  )
lazy val noopJVM = noop.jvm
lazy val noopJS = noop.js

lazy val slf4j = project
  .settings(commonSettings, releaseSettings, mimaSettings)
  .dependsOn(coreJVM)
  .settings(
    name := "log4cats-slf4j",
    libraryDependencies ++= Seq(
      "org.slf4j"                       % "slf4j-api" % slf4jV,
      "org.scala-lang"                  % "scala-reflect" % scalaVersion.value,
      "org.typelevel" %%% "cats-effect" % catsEffectV,
      "ch.qos.logback"                  % "logback-classic" % logbackClassicV % Test
    )
  )

lazy val contributors = Seq(
  "ChristopherDavenport" -> "Christopher Davenport",
  "lorandszakacs" -> "Loránd Szakács"
)

lazy val commonSettings = Seq(
  organization := "io.chrisdavenport",
  scalaVersion := "2.13.2",
  crossScalaVersions := Seq(scalaVersion.value, "2.12.11"),
  addCompilerPlugin("org.typelevel" %% "kind-projector"     % "0.11.0" cross CrossVersion.full),
  addCompilerPlugin("com.olegpy"    %% "better-monadic-for" % "0.3.1"),
  libraryDependencies ++= Seq(
    "org.specs2" %%% "specs2-core" % specs2V % Test
    // "org.specs2"                  %% "specs2-scalacheck"          % specs2V       % Test
  )
)

lazy val releaseSettings = {
  Seq(
    publishArtifact in Test := false,
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/ChristopherDavenport/log4cats"),
        "git@github.com:ChristopherDavenport/log4cats.git"
      )
    ),
    homepage := Some(url("https://github.com/ChristopherDavenport/log4cats")),
    licenses := Seq("Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0.html")),
    publishMavenStyle := true,
    pomIncludeRepository := { _ =>
      false
    },
    pomExtra := {
      <developers>
        {for ((username, name) <- contributors) yield <developer>
          <id>{username}</id>
          <name>{name}</name>
          <url>http://github.com/{username}</url>
        </developer>}
      </developers>
    }
  )
}

lazy val micrositeSettings = Seq(
  micrositeName := "log4cats",
  micrositeDescription := "Functional Logging",
  micrositeAuthor := "Christopher Davenport",
  micrositeGithubOwner := "ChristopherDavenport",
  micrositeGithubRepo := "log4cats",
  micrositeBaseUrl := "/log4cats",
  micrositeDocumentationUrl := "https://christopherdavenport.github.io/log4cats",
  micrositeFooterText := None,
  micrositeHighlightTheme := "atom-one-light",
  micrositePalette := Map(
    "brand-primary" -> "#3e5b95",
    "brand-secondary" -> "#294066",
    "brand-tertiary" -> "#2d5799",
    "gray-dark" -> "#49494B",
    "gray" -> "#7B7B7E",
    "gray-light" -> "#E5E5E6",
    "gray-lighter" -> "#F4F3F4",
    "white-color" -> "#FFFFFF"
  ),
  scalacOptions --= Seq(
    "-Xfatal-warnings",
    "-Ywarn-unused-import",
    "-Ywarn-numeric-widen",
    "-Ywarn-dead-code",
    "-Ywarn-unused:imports",
    "-Xlint:-missing-interpolator,_"
  ),
  micrositePushSiteWith := GitHub4s,
  micrositeGithubToken := sys.env.get("GITHUB_TOKEN")
)

// Not Used Currently
lazy val mimaSettings = {
  def mimaVersion(version: String) = {
    VersionNumber(version) match {
      case VersionNumber(Seq(major, minor, patch, _*), _, _) if patch.toInt > 0 =>
        Some(s"${major}.${minor}.${patch.toInt - 1}")
      case _ =>
        None
    }
  }

  Seq(
    mimaFailOnProblem := mimaVersion(version.value).isDefined,
    mimaFailOnNoPrevious in ThisBuild := false,
    mimaPreviousArtifacts := (mimaVersion(version.value) map {
      organization.value % s"${moduleName.value}_${scalaBinaryVersion.value}" % _
    }).toSet,
    mimaBinaryIssueFilters ++= {
      import com.typesafe.tools.mima.core._
      import com.typesafe.tools.mima.core.ProblemFilters._
      Seq()
    }
  )
}

lazy val noPublishSettings = {
  Seq(
    publish := {},
    publishLocal := {},
    publishArtifact := false
  )
}
