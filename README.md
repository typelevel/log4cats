# log4cats [![Build Status](https://travis-ci.org/ChristopherDavenport/log4cats.svg?branch=master)](https://travis-ci.org/ChristopherDavenport/log4cats) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.chrisdavenport/log4cats_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.chrisdavenport/log4cats_2.12)

## Quick Start

To use linebacker in an existing SBT project with Scala 2.11 or a later version, add the following dependency to your
`build.sbt`:

```scala
libraryDependencies += "io.chrisdavenport" %% "log4cats" % "<version>"
```

## Examples

```scala
import io.chrisdavenport.log4cats.Logger
import cats.effect.Sync
import cats.implicits._

object MyThing {
  // Impure But What 90% of Folks I know do with log4s
  implicit def localLogger[F[_]: Sync] = Logger.createLocal[F]

  // Arbitrary Local Function Declaration
  def doSomething[F[_]: Sync]: F[Unit] =
    Logger[F].info("Logging Start Something") *>
    Sync[F].delay(println("I could be doing anything"))
      .attempt.flatTap{
        case Left(e) => Logger[F].error(e)("Something Went Wrong")
        case Right(_) => Sync[F].pure(())
      }
}
```