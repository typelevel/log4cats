---
layout: home

---
# log4cats [![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.typelevel/log4cats-core_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.typelevel/log4cats-core_2.12)

## Project Goals

log4cats attempts to make referentially transparent logging a reality. These F algebras allow you to write
code for logging knowing you won't be doing side-effects as it offers no way to do so. We provide our own slf4j layer,
or you can use any of the supported backends, or create your own.

## Quick Start

To use log4cats in an existing SBT project with Scala 2.11 or a later version, add the following dependency to your
`build.sbt`:

```scala
libraryDependencies ++= Seq(
  "org.typelevel" %% "log4cats-core"    % "<version>",  // Only if you want to Support Any Backend
  "org.typelevel" %% "log4cats-slf4j"   % "<version>",  // Direct Slf4j Support - Recommended
)
```

## Examples

```scala mdoc
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import cats.effect.{Sync, IO}
import cats.implicits._

object MyThing {
  // Impure But What 90% of Folks I know do with log4s
  implicit def unsafeLogger[F[_]: Sync] = Slf4jLogger.unsafeCreate[F]

  // Arbitrary Local Function Declaration
  def doSomething[F[_]: Sync]: F[Unit] =
    Logger[F].info("Logging Start Something") *>
    Sync[F].delay(println("I could be doing anything"))
      .attempt.flatMap{
        case Left(e) => Logger[F].error(e)("Something Went Wrong")
        case Right(_) => Sync[F].pure(())
      }
}

def safelyDoThings[F[_]: Sync]: F[Unit] = for {
    logger <- Slf4jLogger.create[F]
    _ <- logger.info("Logging at start of safelyDoThings")
    something <- Sync[F].delay(println("I could do anything"))
      .onError{case e => logger.error(e)("Something Went Wrong in safelyDoThings")}
    _ <- logger.info("Logging at end of safelyDoThings")
  } yield something

def passForEasierUse[F[_]: Sync: Logger] = for {
    _ <- Logger[F].info("Logging at start of passForEasierUse")
    something <- Sync[F].delay(println("I could do anything"))
      .onError{case e => Logger[F].error(e)("Something Went Wrong in passForEasierUse")}
    _ <- Logger[F].info("Logging at end of passForEasierUse")
  } yield something
```

### Laconic syntax

It's possible to use interpolated syntax for logging. 
Currently, supported ops are: `trace`, `debug`, `info`, `warn`, `error`.
You can use it for your custom `Logger` as well as for Slf4j `Logger`.

```scala mdoc
import cats.effect.Sync
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.syntax._

def successComputation[F[_]: Sync]: F[Int] = Sync[F].pure(1)
def errorComputation[F[_]: Sync]: F[Unit] = Sync[F].raiseError[Unit](new Throwable("Sorry!"))

def log[F[_]: Sync: Logger] = 
  for {
    result1 <- successComputation[F]
    _ <- info"First result is $result1"
    _ <- errorComputation[F].onError(_ => error"We got an error!")
  } yield ()
```