import com.typesafe.tools.mima.core._

val Scala213 = "2.13.8"
val Scala212 = "2.12.16"
val Scala3 = "3.1.3"

ThisBuild / tlBaseVersion := "2.4"
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

val catsV = "2.7.0"
val catsEffectV = "3.3.14"
val slf4jV = "1.7.36"
val munitCatsEffectV = "1.0.7"
val logbackClassicV = "1.2.11"

Global / onChangedBuildSource := ReloadOnSourceChanges

lazy val root = tlCrossRootProject.aggregate(core, testing, noop, slf4j, docs)

lazy val docs = project
  .in(file("site"))
  .enablePlugins(TypelevelSitePlugin)
  .dependsOn(slf4j)

lazy val core = crossProject(JSPlatform, JVMPlatform)
  .settings(commonSettings)
  .settings(
    name := "log4cats-core",
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "cats-core" % catsV
    ),
    libraryDependencies ++= {
      if (tlIsScala3.value) Seq.empty
      else Seq("org.scala-lang" % "scala-reflect" % scalaVersion.value % Provided)
    }
  )
  .jsSettings(
    // https://www.scala-js.org/news/2022/04/04/announcing-scalajs-1.10.0#fixes-with-compatibility-concerns
    libraryDependencies += ("org.scala-js" %%% "scalajs-java-securerandom" % "1.0.0")
      .cross(CrossVersion.for3Use2_13)
  )

lazy val testing = crossProject(JSPlatform, JVMPlatform)
  .settings(commonSettings)
  .dependsOn(core)
  .settings(
    name := "log4cats-testing",
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "cats-effect" % catsEffectV,
      "ch.qos.logback"                  % "logback-classic" % logbackClassicV % Test
    )
  )

lazy val noop = crossProject(JSPlatform, JVMPlatform)
  .settings(commonSettings)
  .dependsOn(core)
  .settings(
    name := "log4cats-noop"
  )

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

lazy val commonSettings = Seq(
  libraryDependencies ++= Seq(
    "org.typelevel" %%% "munit-cats-effect-3" % munitCatsEffectV % Test
  )
)
