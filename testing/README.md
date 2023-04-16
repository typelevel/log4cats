# Testing with Log4Cats

## Why not just use [`NoOpLogger`](../noop/shared/src/main/scala/org/typelevel/log4cats/noop/NoOpLogger.scala)?

While an error in production is unquestionably the most important time to have good logging, a more common time when
having logs is really useful is figuring out why a test is failing. Unfortunately, logging and test reporting often
aren't synchronized in a useful manner, and the output becomes difficult to decipher.

A common response to this is simply to disable logging in the tests generally, and open it back up locally when
debugging. `NoOpLogger` will certainly do that for you, however there is a better way - integrating log message into
the output of _only_ failing tests.

## How can [`org.typelevel.log4cats.testing`](shared/src/main/scala/org/typelevel/log4cats/testing/) help?

How log4cats will be integrated into test output looks different depending on the test framework. An example of
how to do this for `munit` could look like this:

```scala
package org.log4cats.example

import cats.MonadThrow
import cats.effect.unsafe.IORuntime
import cats.effect.{IO, Resource}
import cats.syntax.all._
import munit.internal.console.Printers
import munit.{CatsEffectSuite, Printer, TestOptions}
import org.typelevel.log4cats.{LoggerFactory, SelfAwareStructuredLogger}
import org.typelevel.log4cats.testing.StructuredTestingLogger
import org.typelevel.log4cats.testing.StructuredTestingLogger.{DEBUG, ERROR, INFO, TRACE, WARN}

import java.io.{PrintWriter, StringWriter}

class ExampleTest extends CatsEffectSuite {
  private val loggerFixture = ResourceFunFixture[LoggingHelper]((options: TestOptions) =>
    Resource.make(IO.delay {
      val logger = StructuredTestingLogger.impl[IO]()
      LoggingHelper(logger, logger.addContext(Map("TestName" -> options.name)))
    })(_ => IO.unit)
  )

  private val loggerPrinter = new Printer {
    override def print(value: Any, out: StringBuilder, indent: Int): Boolean =
      value match {
        case loggingHelper: LoggingHelper =>
          out.appendAll("Logs:")
          val indentation = " " * (indent + 2)
          val innerIndentation = " " * (indent + 8)
          loggingHelper.logged.foreach { log =>
            out
              .append('\n')
              .append(indentation)
              .appendAll(log match {
                case _: TRACE => "TRACE "
                case _: DEBUG => "DEBUG "
                case _: INFO  => "INFO  "
                case _: WARN  => "WARN  "
                case _: ERROR => "ERROR "
              })
              .appendAll(log.message)

            log.ctx.foreach {
              case (k, v) => out.append('\n').appendAll(innerIndentation).appendAll(k).appendAll(" -> ").appendAll(v)
            }

            log.throwOpt.foreach { throwable =>
              val stringWriter = new StringWriter
              throwable.printStackTrace(new PrintWriter(stringWriter))
              stringWriter.toString.split('\n').foreach { line =>
                out.append('\n').appendAll(innerIndentation).appendAll(line)
              }
            }
          }
          true

        case _ => false
      }
  }

  override def munitPrint(clue: => Any): String =
    clue match {
      case message: String => message
      case value => Printers.print(value, loggerPrinter)
    }

  loggerFixture.test("avoid logging for successful tests") { loggingHelper =>
    import loggingHelper.loggerFactory
    CodeUnderTest.logAndDouble[IO]("5").assertEquals(10, loggingHelper)
  }

  loggerFixture.test("output logging for failing tests") { loggingHelper =>
    import loggingHelper.loggerFactory
    CodeUnderTest.logAndDouble[IO]("5.").assertEquals(10, loggingHelper)
  }
}

/**
 * Simple class to reduce some of the boilerplate, and fix the type argument of
 * `StructuredTestingLogger` and avoid an unchecked type cast in `loggerPrinter`
 */
final case class LoggingHelper(underlyingLogger: StructuredTestingLogger[IO],
                               loggerWithContext: SelfAwareStructuredLogger[IO]) {
  implicit val loggerFactory: LoggerFactory[IO] = new LoggerFactory[IO] {
    override def getLoggerFromName(name: String): SelfAwareStructuredLogger[IO] = loggerWithContext
    override def fromName(name: String): IO[SelfAwareStructuredLogger[IO]] = loggerWithContext.pure[IO]
  }

  def logged(implicit runtime:IORuntime): Vector[StructuredTestingLogger.LogMessage] =
    underlyingLogger.logged.unsafeRunSync()
}

object CodeUnderTest {
  def logAndDouble[F[_]: LoggerFactory: MonadThrow](input: String): F[Int] = {
    val logger = LoggerFactory[F].getLogger
    for {
      _ <- logger.info(s"Input $input")
      intVal <- MonadThrow[F].catchNonFatal(input.toInt).recoverWith {
        case ex: NumberFormatException => logger.warn(ex)("Recovering to 0").as(0)
      }
      doubled = intVal * 2
      _ <- logger.info(s"Result: $doubled")
    } yield doubled
  }
}
```

Running this test should produce something close this output:
```
sbt:log4cats-testing> testOnly org.log4cats.example.ExampleTest
[info] compiling 1 Scala source to /log4cats/testing/jvm/target/scala-2.13/test-classes ...
org.log4cats.example.ExampleTest:
  + avoid logging for successful tests 0.073s
==> X org.log4cats.example.ExampleTest.output logging for failing tests  0.093s munit.ComparisonFailException: /log4cats/testing/shared/src/test/scala/org/typelevel/log4cats/ExampleTest.scala:74
73:    import loggingHelper.loggerFactory
74:    CodeUnderTest.logAndDouble[IO]("5.").assertEquals(10, loggingHelper)
75:  }
Logs:
  INFO  Input 5.
        TestName -> output logging for failing tests
  WARN  Recovering to 0
        TestName -> output logging for failing tests
        java.lang.NumberFormatException: For input string: "5."
        	at java.base/java.lang.NumberFormatException.forInputString(NumberFormatException.java:67)
        	at java.base/java.lang.Integer.parseInt(Integer.java:668)
        	at java.base/java.lang.Integer.parseInt(Integer.java:786)
        	at scala.collection.StringOps$.toInt$extension(StringOps.scala:908)
        	at org.log4cats.example.CodeUnderTest$.$anonfun$logAndDouble$3(ExampleTest.scala:98)
        	at recoverWith$extension @ org.log4cats.example.CodeUnderTest$.$anonfun$logAndDouble$2(ExampleTest.scala:98)
        	at flatMap @ org.log4cats.example.CodeUnderTest$.$anonfun$logAndDouble$2(ExampleTest.scala:98)
        	at delay @ org.typelevel.log4cats.testing.StructuredTestingLogger$.org$typelevel$log4cats$testing$StructuredTestingLogger$$appendLogMessage$1(StructuredTestingLogger.scala:73)
        	at flatMap @ org.log4cats.example.CodeUnderTest$.logAndDouble(ExampleTest.scala:97)
        	at flatMap @ munit.CatsEffectAssertions.assertIO(CatsEffectAssertions.scala:52)
        	at flatMap @ munit.internal.NestingChecks$.checkNestingIO(internal.scala:38)
  INFO  Result: 0
        TestName -> output logging for failing tests
=> Obtained
0
=> Diff (- obtained, + expected)
-0
+10
    at munit.Assertions.failComparison(Assertions.scala:274)
    at apply @ munit.CatsEffectAssertions.$anonfun$assertIO$1(CatsEffectAssertions.scala:52)
    at delay @ org.typelevel.log4cats.testing.StructuredTestingLogger$.org$typelevel$log4cats$testing$StructuredTestingLogger$$appendLogMessage$1(StructuredTestingLogger.scala:73)
    at map @ org.log4cats.example.CodeUnderTest$.$anonfun$logAndDouble$4(ExampleTest.scala:102)
    at delay @ org.typelevel.log4cats.testing.StructuredTestingLogger$.org$typelevel$log4cats$testing$StructuredTestingLogger$$appendLogMessage$1(StructuredTestingLogger.scala:73)
    at as @ munit.CatsEffectFunFixtures$ResourceFunFixture$.$anonfun$apply$8(CatsEffectFunFixtures.scala:55)
    at recoverWith$extension @ org.log4cats.example.CodeUnderTest$.$anonfun$logAndDouble$2(ExampleTest.scala:98)
    at flatMap @ org.log4cats.example.CodeUnderTest$.$anonfun$logAndDouble$2(ExampleTest.scala:98)
    at delay @ org.typelevel.log4cats.testing.StructuredTestingLogger$.org$typelevel$log4cats$testing$StructuredTestingLogger$$appendLogMessage$1(StructuredTestingLogger.scala:73)
    at flatMap @ org.log4cats.example.CodeUnderTest$.logAndDouble(ExampleTest.scala:97)
    at flatMap @ munit.CatsEffectAssertions.assertIO(CatsEffectAssertions.scala:52)
    at flatMap @ munit.internal.NestingChecks$.checkNestingIO(internal.scala:38)
[error] Failed: Total 2, Failed 1, Errors 0, Passed 1
[error] Failed tests:
[error] 	org.log4cats.example.ExampleTest
[error] (Test / testOnly) sbt.TestsFailedException: Tests unsuccessful
[error] Total time: 2 s, completed Jan 31, 2023, 9:45:03 PM
```