import com.typesafe.tools.mima.core._

val Scala213 = "2.13.13"
val Scala212 = "2.12.19"
val Scala3 = "3.3.1"

ThisBuild / tlBaseVersion := "2.6"
ThisBuild / crossScalaVersions := Seq(Scala213, Scala212, Scala3)
ThisBuild / scalaVersion := Scala213
ThisBuild / startYear := Some(2018)
ThisBuild / developers := List(
  Developer(
    "christopherdavenport",
    "Christopher Davenport",
    "chris@christopherdavenport.tech",
    new java.net.URL("https://christopherdavenport.github.io/")
  ),
  Developer(
    "lorandszakacs",
    "Loránd Szakács",
    "lorand.szakacs@protonmail.com",
    new java.net.URL("https://github.com/lorandszakacs")
  )
)
ThisBuild / githubWorkflowJavaVersions := Seq(JavaSpec.temurin("8"), JavaSpec.temurin("11"))

ThisBuild / tlVersionIntroduced := Map("3" -> "2.1.1")

val catsV = "2.10.0"
val catsEffectV = "3.5.3"
val slf4jV = "1.7.36"
val munitCatsEffectV = "2.0.0-M4"
val logbackClassicV = "1.2.13"

Global / onChangedBuildSource := ReloadOnSourceChanges

lazy val root = tlCrossRootProject.aggregate(core, testing, noop, slf4j, docs, `js-console`)

lazy val docs = project
  .in(file("site"))
  .enablePlugins(TypelevelSitePlugin)
  .dependsOn(slf4j)

lazy val core = crossProject(JSPlatform, JVMPlatform, NativePlatform)
  .settings(commonSettings)
  .settings(
    name := "log4cats-core",
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "cats-core"       % catsV,
      "org.typelevel" %%% "cats-effect-std" % catsEffectV
    ),
    libraryDependencies ++= {
      if (tlIsScala3.value) Seq.empty
      else Seq("org.scala-lang" % "scala-reflect" % scalaVersion.value % Provided)
    }
  )
  .nativeSettings(commonNativeSettings)

lazy val testing = crossProject(JSPlatform, JVMPlatform, NativePlatform)
  .settings(commonSettings)
  .dependsOn(core)
  .settings(
    name := "log4cats-testing",
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "cats-effect" % catsEffectV,
      "ch.qos.logback"                  % "logback-classic" % logbackClassicV % Test
    )
  )
  .nativeSettings(commonNativeSettings)

lazy val noop = crossProject(JSPlatform, JVMPlatform, NativePlatform)
  .settings(commonSettings)
  .dependsOn(core)
  .settings(
    name := "log4cats-noop"
  ) // migrated to core, so we check that core is compatible with old noop artifacts
  .jvmSettings(
    mimaCurrentClassfiles := (core.jvm / Compile / classDirectory).value
  )
  .jsSettings(
    mimaCurrentClassfiles := (core.js / Compile / classDirectory).value
  )
  .nativeSettings(
    mimaCurrentClassfiles := (core.native / Compile / classDirectory).value
  )
  .nativeSettings(commonNativeSettings)

lazy val slf4j = project
  .settings(commonSettings)
  .dependsOn(core.jvm)
  .settings(
    name := "log4cats-slf4j",
    libraryDependencies ++= Seq(
      "org.slf4j"                       % "slf4j-api"       % slf4jV,
      "org.typelevel" %%% "cats-effect" % catsEffectV,
      "ch.qos.logback"                  % "logback-classic" % logbackClassicV % Test
    ),
    libraryDependencies ++= {
      if (tlIsScala3.value) Seq.empty
      else Seq("org.scala-lang" % "scala-reflect" % scalaVersion.value % Provided)
    }
  )

lazy val `js-console` = project
  .settings(commonSettings)
  .dependsOn(core.js)
  .settings(
    name := "log4cats-js-console",
    tlVersionIntroduced := List("2.12", "2.13", "3").map(_ -> "2.6.0").toMap,
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "cats-effect-kernel" % catsEffectV
    )
  )
  .enablePlugins(ScalaJSPlugin)

lazy val commonSettings = Seq(
  libraryDependencies ++= Seq(
    "org.typelevel" %%% "munit-cats-effect" % munitCatsEffectV % Test
  )
)

lazy val commonNativeSettings = Seq(
  tlVersionIntroduced := List("2.12", "2.13", "3").map(_ -> "2.4.1").toMap
)
