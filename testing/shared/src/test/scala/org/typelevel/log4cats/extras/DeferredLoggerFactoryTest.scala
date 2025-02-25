/*
 * Copyright 2018 Typelevel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.typelevel.log4cats.extras

import cats.Order
import cats.arrow.FunctionK
import cats.data.NonEmptyList
import cats.effect.IO
import cats.syntax.all.*
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.testing.TestingLoggerFactory
import org.typelevel.log4cats.testing.TestingLoggerFactory.{
  Debug,
  Error,
  Info,
  LogMessage as TestLogMessage,
  Trace,
  Warn
}
import org.typelevel.scalaccompat.annotation.nowarn

@nowarn("msg=dead code following this construct")
class DeferredLoggerFactoryTest extends munit.CatsEffectSuite {
  test(
    "DeferredLoggerFactory should not log messages by default when code completes without raising an error"
  ) {
    val testLoggerFactory = TestingLoggerFactory.atomic[IO]()
    DeferredLoggerFactory(testLoggerFactory)
      .use { loggerFactory =>
        def test(logger: SelfAwareStructuredLogger[IO]): IO[Unit] =
          for {
            _ <- logger.info("Test Message 0")
            _ <- testLoggerFactory.logged.assertEquals(Vector.empty)
            _ <- logger.warn("Test Message 1")
            _ <- testLoggerFactory.logged.assertEquals(Vector.empty)
          } yield ()

        (
          test(loggerFactory.getLoggerFromName("Logger 1")),
          test(loggerFactory.getLoggerFromName("Logger 2"))
        ).parTupled.void
      }
      .assert
      .flatMap(_ => testLoggerFactory.logged)
      .assertEquals(Vector.empty)
  }

  test(
    "DeferredLoggerFactory should provide the means to log messages when code completes without raising an error"
  ) {
    val testLoggerFactory = TestingLoggerFactory.atomic[IO]()
    DeferredLoggerFactory(testLoggerFactory)
      .use { loggerFactory =>
        val loggerName = "Test Logger"
        val logger = loggerFactory.getLoggerFromName(loggerName)
        for {
          _ <- logger.trace("Test Message 0")
          _ <- logger.debug("Test Message 1")
          _ <- logger.info("Test Message 2")
          _ <- testLoggerFactory.logged.assertEquals(
            Vector.empty,
            clue("Checking that logging is deferred")
          )
          _ <- loggerFactory.log
          _ <- testLoggerFactory.logged.assertEquals(
            Vector(
              Trace(loggerName, "Test Message 0", none),
              Debug(loggerName, "Test Message 1", none),
              Info(loggerName, "Test Message 2", none)
            ),
            clue("Checking that logs were sent to test logger")
          )
          _ <- loggerFactory.log
          _ <- testLoggerFactory.logged.assertEquals(
            Vector(
              Trace(loggerName, "Test Message 0", none),
              Debug(loggerName, "Test Message 1", none),
              Info(loggerName, "Test Message 2", none)
            ),
            clue("Checking for duplicate logging")
          )
          _ <- logger.warn("Test Message 3")
          _ <- logger.error("Test Message 4")
          _ <- testLoggerFactory.logged.assertEquals(
            Vector(
              Trace(loggerName, "Test Message 0", none),
              Debug(loggerName, "Test Message 1", none),
              Info(loggerName, "Test Message 2", none)
            ),
            clue("Verifying that calling #log does not make the logger eager")
          )
          _ <- loggerFactory.log
          _ <- testLoggerFactory.logged.assertEquals(
            Vector(
              Trace(loggerName, "Test Message 0", none),
              Debug(loggerName, "Test Message 1", none),
              Info(loggerName, "Test Message 2", none),
              Warn(loggerName, "Test Message 3", none),
              Error(loggerName, "Test Message 4", none)
            )
          )
        } yield ()
      }
      .assert
      .flatMap(_ => testLoggerFactory.logged)
      .assertEquals(
        Vector(
          Trace("Test Logger", "Test Message 0", none),
          Debug("Test Logger", "Test Message 1", none),
          Info("Test Logger", "Test Message 2", none),
          Warn("Test Logger", "Test Message 3", none),
          Error("Test Logger", "Test Message 4", none)
        )
      )
  }

  test("DeferredLoggerFactory should log messages from all loggers when logging is requested") {
    val testLoggerFactory = TestingLoggerFactory.atomic[IO]()
    DeferredLoggerFactory(testLoggerFactory)
      .use { loggerFactory =>
        def logStuff(logger: SelfAwareStructuredLogger[IO]): IO[Unit] =
          for {
            _ <- logger.trace("Test Message")
            _ <- logger.warn("Test Message")
          } yield ()

        (0 until 5).toVector
          .parTraverse_ { idx =>
            logStuff(loggerFactory.getLoggerFromName(s"Logger $idx"))
          }
          .flatTap(_ => loggerFactory.log)
      }
      .assert
      .flatMap(_ => testLoggerFactory.logged)
      .map(
        _.sorted(
          Order
            .whenEqual[TestLogMessage](
              Order.by(_.loggerName),
              Order.by(_.level)
            )
            .toOrdering
        )
      )
      .assertEquals(
        Vector(
          Trace("Logger 0", "Test Message", None, Map.empty),
          Warn("Logger 0", "Test Message", None, Map.empty),
          Trace("Logger 1", "Test Message", None, Map.empty),
          Warn("Logger 1", "Test Message", None, Map.empty),
          Trace("Logger 2", "Test Message", None, Map.empty),
          Warn("Logger 2", "Test Message", None, Map.empty),
          Trace("Logger 3", "Test Message", None, Map.empty),
          Warn("Logger 3", "Test Message", None, Map.empty),
          Trace("Logger 4", "Test Message", None, Map.empty),
          Warn("Logger 4", "Test Message", None, Map.empty)
        )
      )
  }

  test("DeferredLoggerFactory should not lose log messages when an exception is raised") {
    val testLoggerFactory = TestingLoggerFactory.atomic[IO]()
    DeferredLoggerFactory(testLoggerFactory)
      .use { loggerFactory =>
        val happyPath =
          for {
            logger <- loggerFactory.fromName("Happy Path")
            _ <- logger.info("Test Message 1")
            _ <- logger.debug("Test Message 2")
          } yield ()

        val sadPath0 =
          for {
            logger <- loggerFactory.fromName("Sad Path 0")
            _ <- logger.info("Test Message")
            _ <- testLoggerFactory.logged.assertEquals(Vector.empty)
            _ <- IO.raiseError(new RuntimeException("Expected Exception 0"))
          } yield ()

        val sadPath1 =
          for {
            logger <- loggerFactory.fromName("Sad Path 1")
            _ <- logger.warn("Test Message 1")
            _ <- logger.error("Test Message 2")
            _ <- testLoggerFactory.logged.assertEquals(Vector.empty)
            _ <- IO.raiseError(new RuntimeException("Expected Exception 1"))
          } yield ()

        (
          sadPath0.attempt.map(_.toValidatedNel),
          happyPath.attempt.map(_.toValidatedNel),
          sadPath1.attempt.map(_.toValidatedNel)
        ).parMapN(_ combine _ combine _).flatTap(_ => loggerFactory.log)
      }
      .flatTap(value =>
        IO.delay {
          assertEquals(
            value.leftMap(_.map(_.getMessage)),
            NonEmptyList
              .of(
                "Expected Exception 0",
                "Expected Exception 1"
              )
              .invalid,
            clue(value)
          )
        }
      )
      .flatMap(_ => testLoggerFactory.logged)
      // Have to sort because of the parTupled
      .map(_.sortBy(l => l.loggerName -> l.message))
      .assertEquals(
        Vector(
          Info("Happy Path", "Test Message 1", None, Map.empty),
          Debug("Happy Path", "Test Message 2", None, Map.empty),
          Info("Sad Path 0", "Test Message", None, Map.empty),
          Warn("Sad Path 1", "Test Message 1", None, Map.empty),
          Error("Sad Path 1", "Test Message 2", None, Map.empty)
        )
      )
  }

  test("DeferredLoggerFactory should not duplicate log messages when an exception is raised") {
    TestingLoggerFactory
      .ref[IO]()
      .flatMap { testLoggerFactory =>
        DeferredLoggerFactory(testLoggerFactory)
          .use { loggerFactory =>
            val happyPath =
              for {
                logger <- loggerFactory.fromName("Happy Path")
                _ <- logger.info("Test Message 1")
                _ <- logger.debug("Test Message 2")
                _ <- loggerFactory.log
              } yield ()

            val sadPath0 =
              for {
                logger <- loggerFactory.fromName("Sad Path 0")
                _ <- logger.info("Test Message")
                _ <- IO.raiseError(new RuntimeException("Expected Exception 0"))
              } yield ()

            val sadPath1 =
              for {
                logger <- loggerFactory.fromName("Sad Path 1")
                _ <- logger.warn("Test Message 1")
                _ <- loggerFactory.log
                _ <- logger.error("Test Message 2")
                _ <- IO.raiseError(new RuntimeException("Expected Exception 1"))
              } yield ()

            (
              sadPath0.attempt.map(_.toValidatedNel),
              happyPath.attempt.map(_.toValidatedNel),
              sadPath1.attempt.map(_.toValidatedNel)
            ).parMapN(_ combine _ combine _)
              .flatTap(_ => loggerFactory.log)
          }
          .flatTap(value =>
            IO.delay {
              assertEquals(
                value.leftMap(_.map(_.getMessage)),
                NonEmptyList
                  .of(
                    "Expected Exception 0",
                    "Expected Exception 1"
                  )
                  .invalid,
                clue(value)
              )
            }
          )
          .flatMap(_ => testLoggerFactory.logged)
      }
      // Have to sort because of the parTupled
      .map(_.sortBy(l => l.loggerName -> l.message))
      .assertEquals(
        Vector(
          Info("Happy Path", "Test Message 1", None, Map.empty),
          Debug("Happy Path", "Test Message 2", None, Map.empty),
          Info("Sad Path 0", "Test Message", None, Map.empty),
          Warn("Sad Path 1", "Test Message 1", None, Map.empty),
          Error("Sad Path 1", "Test Message 2", None, Map.empty)
        )
      )
  }

  test("DeferredLoggerFactory should respect log levels") {
    val testLoggerFactory = TestingLoggerFactory.atomic[IO](
      debugEnabled = false
    )
    DeferredLoggerFactory(testLoggerFactory).use { loggerFactory =>
      val loggerName = "Test Logger"
      val logger = loggerFactory.getLoggerFromName(loggerName)
      for {
        _ <- logger.trace("Test Message 0")
        _ <- logger.debug("Test Message 1")
        _ <- logger.info("Test Message 2")
        _ <- testLoggerFactory.logged.assertEquals(
          Vector.empty,
          clue("Checking that logging is deferred")
        )
        _ <- loggerFactory.inspect
          .map(_.toVector)
          .assertEquals(
            Vector(
              DeferredLogMessage.trace(Map.empty, none, () => "Test Message 0"),
              DeferredLogMessage.info(Map.empty, none, () => "Test Message 2")
            ),
            clue("Checking that the debug message was not buffered")
          )
        _ <- loggerFactory.log
        _ <- testLoggerFactory.logged.assertEquals(
          Vector(
            Trace(loggerName, "Test Message 0", none),
            Info(loggerName, "Test Message 2", none)
          ),
          clue("Checking that logs were sent to test logger")
        )
      } yield ()
    }.assert
  }

  test("DeferredLoggerFactory doesn't lose the ability to log when message is modified") {
    val testLoggerFactory = TestingLoggerFactory.atomic[IO]()
    DeferredLoggerFactory(testLoggerFactory)
      .map(_.withModifiedString(msg => s"[DLF]$msg"))
      .use { loggerFactory =>
        def logStuff(logger: SelfAwareStructuredLogger[IO]): IO[Unit] =
          for {
            _ <- logger.trace("Test Message")
            _ <- logger.withModifiedString(msg => s"[DSL]$msg").warn("Test Message")
          } yield ()

        (0 until 5).toVector
          .parTraverse_ { idx =>
            logStuff(loggerFactory.getLoggerFromName(s"Logger $idx"))
          }
          .flatTap(_ => loggerFactory.log)
      }
      .assert
      .flatMap(_ => testLoggerFactory.logged)
      .map(
        _.sorted(
          Order
            .whenEqual[TestLogMessage](
              Order.by(_.loggerName),
              Order.by(_.level)
            )
            .toOrdering
        )
      )
      .assertEquals(
        Vector(
          Trace("Logger 0", "[DLF]Test Message", None, Map.empty),
          Warn("Logger 0", "[DLF][DSL]Test Message", None, Map.empty),
          Trace("Logger 1", "[DLF]Test Message", None, Map.empty),
          Warn("Logger 1", "[DLF][DSL]Test Message", None, Map.empty),
          Trace("Logger 2", "[DLF]Test Message", None, Map.empty),
          Warn("Logger 2", "[DLF][DSL]Test Message", None, Map.empty),
          Trace("Logger 3", "[DLF]Test Message", None, Map.empty),
          Warn("Logger 3", "[DLF][DSL]Test Message", None, Map.empty),
          Trace("Logger 4", "[DLF]Test Message", None, Map.empty),
          Warn("Logger 4", "[DLF][DSL]Test Message", None, Map.empty)
        )
      )
  }

  test("DeferredLoggerFactory doesn't lose the ability to log when mapK is called") {
    val testLoggerFactory = TestingLoggerFactory.atomic[IO]()
    DeferredLoggerFactory(testLoggerFactory)
      .map(_.mapK(FunctionK.id[IO]))
      .use { loggerFactory =>
        val logger = loggerFactory.getLoggerFromName("Test Logger")
        for {
          _ <- logger.trace("Test Message 0")
          _ <- logger.debug("Test Message 1")
          _ <- logger.info("Test Message 2")
          _ <- testLoggerFactory.logged.assertEquals(
            Vector.empty,
            clue("Checking that logging is deferred")
          )
          _ <- loggerFactory.log
        } yield ()
      }
      .assert
      .flatMap(_ => testLoggerFactory.logged)
      .assertEquals(
        Vector(
          Trace("Test Logger", "Test Message 0", none),
          Debug("Test Logger", "Test Message 1", none),
          Info("Test Logger", "Test Message 2", none)
        )
      )
  }

  test("DeferredLoggerFactory doesn't lose the ability to log when context is added") {
    val testLoggerFactory = TestingLoggerFactory.atomic[IO]()
    val factoryCtx = List("factory" -> "added").toMap
    def loggerCtx(idx: Int) = List(s"logger $idx" -> "added").toMap
    def msgCtx(idx: Int) = List(s"log $idx" -> "added").toMap
    DeferredLoggerFactory(testLoggerFactory)
      .map(_.addContext(factoryCtx))
      .use { loggerFactory =>
        (0 until 5).toVector
          .parTraverse_ { idx =>
            loggerFactory
              .getLoggerFromName(s"Logger $idx")
              .addContext(loggerCtx(idx))
              .trace(msgCtx(idx))("Test Message")
          }
          .flatTap(_ => loggerFactory.log)
      }
      .assert
      .flatMap(_ => testLoggerFactory.logged)
      .map(
        _.sorted(
          Order
            .whenEqual[TestLogMessage](
              Order.by(_.loggerName),
              Order.by(_.level)
            )
            .toOrdering
        )
      )
      .assertEquals(
        Vector(
          Trace(
            "Logger 0",
            "Test Message",
            None,
            factoryCtx ++ loggerCtx(0) ++ msgCtx(0)
          ),
          Trace(
            "Logger 1",
            "Test Message",
            None,
            factoryCtx ++ loggerCtx(1) ++ msgCtx(1)
          ),
          Trace(
            "Logger 2",
            "Test Message",
            None,
            factoryCtx ++ loggerCtx(2) ++ msgCtx(2)
          ),
          Trace(
            "Logger 3",
            "Test Message",
            None,
            factoryCtx ++ loggerCtx(3) ++ msgCtx(3)
          ),
          Trace("Logger 4", "Test Message", None, factoryCtx ++ loggerCtx(4) ++ msgCtx(4))
        )
      )
  }
}
