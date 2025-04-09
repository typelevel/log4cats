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

package org.typelevel.log4cats

import cats.effect.IO
import cats.mtl.Local
import munit.CatsEffectSuite
import org.typelevel.log4cats.extras.LogLevel

import scala.annotation.nowarn

class LocalLoggerTest extends CatsEffectSuite {
  private[this] def localTest(
      name: String
  )(body: Local[IO, Map[String, String]] => IO[?]): Unit =
    test(name) {
      IO.local(Map.empty[String, String]).flatMap(body)
    }

  private[this] def factoryTest(
      name: String
  )(body: (LocalLoggerFactory[IO], TestLogger.Output[IO]) => IO[?]): Unit =
    localTest(name) { implicit local =>
      TestLogger.Factory().flatMap { factory =>
        body(LocalLoggerFactory.fromLocal(factory), factory.output)
      }
    }

  private[this] def loggerTest(
      name: String
  )(body: (LocalLogger[IO], TestLogger[IO]) => IO[?]): Unit =
    localTest(name) { implicit local =>
      TestLogger[IO]("test").flatMap { logger =>
        body(LocalLogger.fromLocal(logger), logger)
      }
    }

  factoryTest("LocalLoggerFactory stores context locally") { (factory, output) =>
    factory.withAddedContext(Map("foo" -> "1")) {
      for {
        logger <- factory.fromName("test")
        _ <- logger.info("bar")
        entries <- output.loggedEntries
      } yield {
        assertEquals(entries.length, 1)
        assertEquals(
          entries.head,
          TestLogger.Entry(
            loggerName = "test",
            level = LogLevel.Info,
            message = "bar",
            exception = None,
            context = Map("foo" -> "1")
          )
        )
      }
    }
  }

  factoryTest("context stored after creation of logger is visible to logger") { (factory, output) =>
    for {
      logger <- factory.fromName("test")
      _ <- factory.withAddedContext(Map("foo" -> "1")) {
        for {
          _ <- logger.error("bar")
          entries <- output.loggedEntries
        } yield {
          assertEquals(entries.length, 1)
          assertEquals(
            entries.head,
            TestLogger.Entry(
              loggerName = "test",
              level = LogLevel.Error,
              message = "bar",
              exception = None,
              context = Map("foo" -> "1")
            )
          )
        }
      }
    } yield ()
  }

  factoryTest("log site context overrides local context") { (factory, output) =>
    factory.withAddedContext("shared" -> "local", "foo" -> "1") {
      for {
        logger <- factory.fromName("test")
        _ <- logger.warn(Map("shared" -> "log site", "bar" -> "1"))("baz")
        entries <- output.loggedEntries
      } yield {
        assertEquals(entries.length, 1)
        assertEquals(
          entries.head,
          TestLogger.Entry(
            loggerName = "test",
            level = LogLevel.Warn,
            message = "baz",
            exception = None,
            context = Map(
              "shared" -> "log site",
              "foo" -> "1",
              "bar" -> "1"
            )
          )
        )
      }
    }
  }

  loggerTest("LocalLogger respects disabled log levels") { (localLogger, testLogger) =>
    localLogger.withAddedContext(Map("foo" -> "1")) {
      for {
        _ <- testLogger.enableLoggingWithFinestLevel(LogLevel.Debug)
        _ <- localLogger.trace("bar")
        _ <- localLogger.debug("baz")
        _ <- testLogger.disableLogging
        _ <- localLogger.error("qux")
        entries <- testLogger.output.loggedEntries
      } yield {
        assertEquals(entries.length, 1)
        assertEquals(
          entries.head,
          TestLogger.Entry(
            loggerName = "test",
            level = LogLevel.Debug,
            message = "baz",
            exception = None,
            context = Map("foo" -> "1")
          )
        )
      }
    }
  }

  loggerTest("context stored locally is visible to deprecated StructuredLogger view") {
    (localLogger, testLogger) =>
      val deprecatedLogger = localLogger.asStructuredLogger: @nowarn("cat=deprecation")
      val structuredLogger = deprecatedLogger.addContext("shared" -> "structured", "foo" -> "1")
      localLogger.withAddedContext(Map("shared" -> "local", "bar" -> "1")) {
        for {
          _ <- structuredLogger.trace("baz")
          entries <- testLogger.output.loggedEntries
        } yield {
          assertEquals(entries.length, 1)
          assertEquals(
            entries.head,
            TestLogger.Entry(
              loggerName = "test",
              level = LogLevel.Trace,
              message = "baz",
              exception = None,
              context = Map(
                "shared" -> "local",
                "foo" -> "1",
                "bar" -> "1"
              )
            )
          )
        }
      }
  }
}
