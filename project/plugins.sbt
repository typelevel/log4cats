addSbtPlugin("org.scala-js"     % "sbt-scalajs"      % "1.23.0")
addSbtPlugin("org.scala-native" % "sbt-scala-native" % "0.5.12")
addSbtPlugin("com.github.sbt"   % "sbt-jmh"          % "0.4.9")
val sbtTypelevelVersion = "0.8.7"
addSbtPlugin("org.typelevel" % "sbt-typelevel"      % sbtTypelevelVersion)
addSbtPlugin("org.typelevel" % "sbt-typelevel-site" % sbtTypelevelVersion)
