# log4cats [![Build Status](https://github.com/typelevel/log4cats/workflows/Continuous%20Integration/badge.svg?branch=main)](https://github.com/typelevel/log4cats/actions?query=branch%3Amain+workflow%3A%22Continuous+Integration%22) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.typelevel/log4cats-core_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.typelevel/log4cats-core_2.12)

## Project Goals

log4cats attempts to make referentially transparent logging a reality. These F algebras allow you to write
code for logging knowing you won't be doing side-effects as it offers no way to do so. We provide our own slf4j layer,
or you can use any of the supported backends, or create your own.

## Quick Start

To use log4cats in an existing SBT project with Scala 2.12 or a later version, add the following dependency to your
`build.sbt`:

```scala
libraryDependencies ++= Seq(
  "org.typelevel" %% "log4cats-core"    % "<version>",  // Only if you want to Support Any Backend
  "org.typelevel" %% "log4cats-slf4j"   % "<version>",  // Direct Slf4j Support - Recommended
)
```

## Why log4cats?

Well, to answer that, let's take a look at how you might combine cats-effect with vanilla logging...

```scala
object MyVanillaLoggingThing {
  val logger: Logger = Logger(LoggerFactory.getLogger(getClass.getName))
  
  def doSomething(): IO[Unit] =
    IO(logger.info("Doing something!")) *> IO.println("Hello, World!")

}
```

But what if you don't want to wrap your logger in an `IO` like this?  
Good news, you don't have to!  Enter log4cats!  Read on!

## Examples

```scala
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import cats.effect.Sync
import cats.syntax.all.*

object MyThing {
  // Impure But What 90% of Folks I know do with log4s
  implicit def unsafeLogger[F[_]: Sync] = Slf4jLogger.getLogger[F]

  // Arbitrary Local Function Declaration
  def doSomething[F[_]: Sync]: F[Unit] =
    Logger[F].info("Logging Start Something") *>
            Sync[F].delay(println("I could be doing anything"))
                    .attempt.flatMap {
                      case Left(e) => Logger[F].error(e)("Something Went Wrong")
                      case Right(_) => Sync[F].pure(())
                    }

  def safelyDoThings[F[_]: Sync]: F[Unit] = for {
    logger <- Slf4jLogger.create[F]
    _ <- logger.info("Logging at start of safelyDoThings")
    something <- Sync[F].delay(println("I could do anything"))
            .onError { case e => logger.error(e)("Something Went Wrong in safelyDoThings") }
    _ <- logger.info("Logging at end of safelyDoThings")
  } yield something

  def passForEasierUse[F[_]: Sync : Logger] = for {
    _ <- Logger[F].info("Logging at start of passForEasierUse")
    something <- Sync[F].delay(println("I could do anything"))
            .onError { case e => Logger[F].error(e)("Something Went Wrong in passForEasierUse") }
    _ <- Logger[F].info("Logging at end of passForEasierUse")
  } yield something
}
```

## Wait...why log4cats again?
If you compare the vanilla logger + cats-effect example with the log4cats examples above,
you might be asking yourself "why do I have to do any of this?" 
or "why can't I just add a log statement like normal?"  

Well there are several reasons.  Logging is often times an overlooked side-effect, 
which under the hood can range from writing to a mutable queue, writing to disk, 
outputting to the console, or sometimes even doing network I/O! 
To correctly deal with these kinds of side-effects we have to ensure they are properly wrapped in `IO`,
see the [cats-effect docs](https://typelevel.org/cats-effect/docs/concepts#side-effects) for more details.

Basically, we are using cats-effect.  We like things like "referential transparency" 
and "programs-as-values".  Wrapping our log statement in an `IO` helps with that.

### Laconic syntax

It's possible to use interpolated syntax for logging.
Currently, supported ops are: `trace`, `debug`, `info`, `warn`, `error`.
You can use it for your custom `Logger` as well as for Slf4j `Logger`.

```scala
import cats.Applicative
import cats.effect.Sync
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.syntax.*

def successComputation[F[_]: Applicative]: F[Int] = Applicative[F].pure(1)
def errorComputation[F[_]: Sync]: F[Unit] = Sync[F].raiseError[Unit](new Throwable("Sorry!"))

def log[F[_]: Sync : Logger] =
  for {
    result1 <- successComputation[F]
    _ <- info"First result is $result1"
    _ <- errorComputation[F].onError(_ => error"We got an error!")
  } yield ()
```

## Logging using capabilities

You can work with logging using capabilities. It's implemented via the `LoggerFactory` trait.
You instantiate it once (dependent on the specific logging backend you use)
and pass this around in your application.

This brings several advantages:

* it's no more needed to pass the very powerful `F[_]: Sync` constraint everywhere 
  that can do almost anything when you only need logging.
* you have control of loggers creation, and you can even add in whatever custom
  functionality you need for your applications here. E.g. create loggers that also push logs
  to some external providers by giving a custom implementation of this trait.

If you are unsure how to create a new `LoggerFactory[F]` instance, then you can look at the `log4cats-slf4j`,
or `log4cats-noop` modules for concrete implementations.

The quickest fix might be to import needed implicits:

```scala
// assumes dependency on log4cats-slf4j module

import org.typelevel.log4cats.*
import org.typelevel.log4cats.slf4j.*

val logger: SelfAwareStructuredLogger[IO] = LoggerFactory[IO].getLogger

// or
def anyFSyncLogger[F[_]: Sync]: SelfAwareStructuredLogger[F] = Slf4jFactory[F].getLogger
```

Alternatively, a mutually exclusive solution is to explicitly create your
`LoggerFactory[F]` instance and pass them around implicitly:

```scala
import cats.effect.IO
import cats.Monad
import cats.syntax.all.*
import org.typelevel.log4cats.*
import org.typelevel.log4cats.slf4j.Slf4jFactory

// create our LoggerFactory
implicit val logging: LoggerFactory[IO] = Slf4jFactory[IO]

// we summon LoggerFactory instance, and create logger
val logger: SelfAwareStructuredLogger[IO] = LoggerFactory[IO].getLogger
logger.info("logging in IO!"): IO[Unit]

// basic example of a service using LoggerFactory
class LoggerUsingService[F[_]: LoggerFactory : Monad] {
  val logger = LoggerFactory[F].getLogger

  def use(args: String): F[Unit] =
    for {
      _ <- logger.info("yay! effect polymorphic code")
      _ <- logger.debug(s"and $args")
    } yield ()
}

new LoggerUsingService[IO].use("foo")
```

## Using log4cats in tests

See [here](testing/README.md) for details

## CVE-2021-44228 ("log4shell")

log4cats is not directly susceptible to CVS-2021-44228.  The
log4cats-slf4j implementation delegates all logging operations to
[slf4j][slf4j].  if you use log4cats-slf4j, your configured slf4j
provider may put you at risk.  See [slf4j's comments on
CVE-2021-44228][slf4j-log4shell] for more.

[slf4j]: https://www.slf4j.org/
[slf4j-log4shell]: https://www.slf4j.org/log4shell.html
