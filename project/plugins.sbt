addSbtPlugin("org.scala-js"       % "sbt-scalajs"                   % "1.20.1")
addSbtPlugin("org.scala-native"   % "sbt-scala-native"              % "0.5.9")
addSbtPlugin("org.portable-scala" % "sbt-scala-native-crossproject" % "1.3.2")
addSbtPlugin("pl.project13.scala" % "sbt-jmh"                       % "0.4.8")
val sbtTypelevelVersion = "0.8.2"
addSbtPlugin("org.typelevel" % "sbt-typelevel"      % sbtTypelevelVersion)
addSbtPlugin("org.typelevel" % "sbt-typelevel-site" % sbtTypelevelVersion)
