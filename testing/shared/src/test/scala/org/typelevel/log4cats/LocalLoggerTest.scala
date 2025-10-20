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
import org.typelevel.log4cats.testing.{StructuredTestingLogger, TestingLoggerFactory}

import scala.annotation.nowarn

class LocalLoggerTest extends CatsEffectSuite {
  private[this] def localTest(
      name: String
  )(body: Local[IO, Map[String, String]] => IO[?]): Unit =
    test(name) {
      IO.local(Map.empty[String, String]).flatMap(body)
    }

  private[this] def factoryTest(
      name: String,
      traceEnabled: Boolean = true,
      debugEnabled: Boolean = true,
      infoEnabled: Boolean = true,
      warnEnabled: Boolean = true,
      errorEnabled: Boolean = true
  )(body: (LocalLoggerFactory[IO], TestingLoggerFactory[IO]) => IO[?]): Unit =
    localTest(name) { implicit local =>
      TestingLoggerFactory
        .ref[IO](
          traceEnabled = traceEnabled,
          debugEnabled = debugEnabled,
          infoEnabled = infoEnabled,
          warnEnabled = warnEnabled,
          errorEnabled = errorEnabled
        )
        .flatMap { factory =>
          body(LocalLoggerFactory.fromLocal(factory), factory)
        }
    }

  private[this] def loggerTest(
      name: String,
      traceEnabled: Boolean = true,
      debugEnabled: Boolean = true,
      infoEnabled: Boolean = true,
      warnEnabled: Boolean = true,
      errorEnabled: Boolean = true
  )(body: (LocalLogger[IO], StructuredTestingLogger[IO]) => IO[?]): Unit =
    localTest(name) { implicit local =>
      StructuredTestingLogger
        .ref[IO](
          traceEnabled = traceEnabled,
          debugEnabled = debugEnabled,
          infoEnabled = infoEnabled,
          warnEnabled = warnEnabled,
          errorEnabled = errorEnabled
        )
        .flatMap { logger =>
          body(LocalLogger.fromLocal(logger), logger)
        }
    }

  factoryTest("LocalLoggerFactory stores context locally") { (localFactory, testFactory) =>
    localFactory.withAddedContext(Map("foo" -> "1")) {
      for {
        logger <- localFactory.fromName("test")
        _ <- logger.info("bar")
        entries <- testFactory.logged
      } yield {
        assertEquals(entries.length, 1)
        assertEquals(
          entries.head,
          TestingLoggerFactory.Info(
            loggerName = "test",
            message = "bar",
            throwOpt = None,
            ctx = Map("foo" -> "1")
          )
        )
      }
    }
  }

  factoryTest("context stored after creation of logger is visible to logger") {
    (localFactory, testFactory) =>
      for {
        logger <- localFactory.fromName("test")
        _ <- localFactory.withAddedContext(Map("foo" -> "1")) {
          for {
            _ <- logger.error("bar")
            entries <- testFactory.logged
          } yield {
            assertEquals(entries.length, 1)
            assertEquals(
              entries.head,
              TestingLoggerFactory.Error(
                loggerName = "test",
                message = "bar",
                throwOpt = None,
                ctx = Map("foo" -> "1")
              )
            )
          }
        }
      } yield ()
  }

  factoryTest("log site context overrides local context") { (localFactory, testFactory) =>
    localFactory.withAddedContext("shared" -> "local", "foo" -> "1") {
      for {
        logger <- localFactory.fromName("test")
        _ <- logger.warn(Map("shared" -> "log site", "bar" -> "1"))("baz")
        entries <- testFactory.logged
      } yield {
        assertEquals(entries.length, 1)
        assertEquals(
          entries.head,
          TestingLoggerFactory.Warn(
            loggerName = "test",
            message = "baz",
            throwOpt = None,
            ctx = Map(
              "shared" -> "log site",
              "foo" -> "1",
              "bar" -> "1"
            )
          )
        )
      }
    }
  }

  loggerTest("LocalLogger respects disabled log levels", traceEnabled = false) {
    (localLogger, testLogger) =>
      localLogger.withAddedContext(Map("foo" -> "1")) {
        for {
          _ <- localLogger.trace("bar")
          _ <- localLogger.debug("baz")
          entries <- testLogger.logged
        } yield {
          assertEquals(entries.length, 1)
          assertEquals(
            entries.head,
            StructuredTestingLogger.DEBUG(
              message = "baz",
              throwOpt = None,
              ctx = Map("foo" -> "1")
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
          entries <- testLogger.logged
        } yield {
          assertEquals(entries.length, 1)
          assertEquals(
            entries.head,
            StructuredTestingLogger.TRACE(
              message = "baz",
              throwOpt = None,
              ctx = Map(
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
