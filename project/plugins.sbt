addSbtPlugin("org.scala-js"       % "sbt-scalajs"                   % "1.14.0")
addSbtPlugin("org.scala-native"   % "sbt-scala-native"              % "0.4.15")
addSbtPlugin("org.portable-scala" % "sbt-scala-native-crossproject" % "1.3.2")
addSbtPlugin("pl.project13.scala" % "sbt-jmh"                       % "0.4.6")
val sbtTypelevelVersion = "0.5.4"
addSbtPlugin("org.typelevel" % "sbt-typelevel"      % sbtTypelevelVersion)
addSbtPlugin("org.typelevel" % "sbt-typelevel-site" % sbtTypelevelVersion)
