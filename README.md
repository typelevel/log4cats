# log4cats [![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.typelevel/log4cats-core_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.typelevel/log4cats-core_2.12)

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

## Examples

```scala
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import cats.effect.Sync
import cats.implicits._

object MyThing {
  // Impure But What 90% of Folks I know do with log4s
  implicit def unsafeLogger[F[_]: Sync] = Slf4jLogger.getLogger[F]

  // Arbitrary Local Function Declaration
  def doSomething[F[_]: Sync]: F[Unit] =
    Logger[F].info("Logging Start Something") *>
    Sync[F].delay(println("I could be doing anything"))
      .attempt.flatMap{
        case Left(e) => Logger[F].error(e)("Something Went Wrong")
        case Right(_) => Sync[F].pure(())
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
}
```

## CVE-2021-44228 ("log4shell")

log4cats is not directly susceptible to CVS-2021-44228.  The
log4cats-slf4j implementation delegates all logging operations to
[slf4j][slf4j].  if you use log4cats-slf4j, your configured slf4j
provider may put you at risk.  See [slf4j's comments on
CVE-2021-44228][slf4j-log4shell] for more.

[slf4j]: https://www.slf4j.org/
[slf4j-log4shell]: https://www.slf4j.org/log4shell.html
