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

import cats.arrow.FunctionK
import cats.effect.IO
import cats.syntax.all._
import org.typelevel.log4cats.testing.StructuredTestingLogger
import org.typelevel.scalaccompat.annotation.nowarn

@nowarn("msg=dead code following this construct")
class DeferredStructuredLoggerTest extends munit.CatsEffectSuite {
  test(
    "DeferredStructuredLogger should not log messages by default when code completes without raising an error"
  ) {
    val testLogger = StructuredTestingLogger.impl[IO]()
    DeferredStructuredLogger(testLogger)
      .use { logger =>
        for {
          _ <- logger.info("Test Message 0")
          _ <- testLogger.logged.assertEquals(Vector.empty)
          _ <- logger.warn("Test Message 1")
          _ <- testLogger.logged.assertEquals(Vector.empty)
        } yield ()
      }
      .assert
      .flatMap(_ => testLogger.logged)
      .assertEquals(Vector.empty)
  }

  test(
    "DeferredStructuredLogger should provide the means to log messages when code completes without raising an error"
  ) {
    val testLogger = StructuredTestingLogger.impl[IO]()
    DeferredStructuredLogger(testLogger)
      .use { logger =>
        for {
          _ <- logger.trace("Test Message 0")
          _ <- logger.debug("Test Message 1")
          _ <- logger.info("Test Message 2")
          _ <- testLogger.logged.assertEquals(
            Vector.empty,
            clue("Checking that logging is deferred")
          )
          _ <- logger.log
          _ <- testLogger.logged.assertEquals(
            Vector(
              StructuredTestingLogger.TRACE("Test Message 0", none),
              StructuredTestingLogger.DEBUG("Test Message 1", none),
              StructuredTestingLogger.INFO("Test Message 2", none)
            ),
            clue("Checking that logs were sent to test logger")
          )
          _ <- logger.log
          _ <- testLogger.logged.assertEquals(
            Vector(
              StructuredTestingLogger.TRACE("Test Message 0", none),
              StructuredTestingLogger.DEBUG("Test Message 1", none),
              StructuredTestingLogger.INFO("Test Message 2", none)
            ),
            clue("Checking for duplicate logging")
          )
          _ <- logger.warn("Test Message 3")
          _ <- logger.error("Test Message 4")
          _ <- testLogger.logged.assertEquals(
            Vector(
              StructuredTestingLogger.TRACE("Test Message 0", none),
              StructuredTestingLogger.DEBUG("Test Message 1", none),
              StructuredTestingLogger.INFO("Test Message 2", none)
            ),
            clue("Verifying that calling #log does not make the logger eager")
          )
          _ <- logger.log
          _ <- testLogger.logged.assertEquals(
            Vector(
              StructuredTestingLogger.TRACE("Test Message 0", none),
              StructuredTestingLogger.DEBUG("Test Message 1", none),
              StructuredTestingLogger.INFO("Test Message 2", none),
              StructuredTestingLogger.WARN("Test Message 3", none),
              StructuredTestingLogger.ERROR("Test Message 4", none)
            )
          )
        } yield ()
      }
      .assert
      .flatMap(_ => testLogger.logged)
      .assertEquals(
        Vector(
          StructuredTestingLogger.TRACE("Test Message 0", none),
          StructuredTestingLogger.DEBUG("Test Message 1", none),
          StructuredTestingLogger.INFO("Test Message 2", none),
          StructuredTestingLogger.WARN("Test Message 3", none),
          StructuredTestingLogger.ERROR("Test Message 4", none)
        )
      )
  }

  test("DeferredStructuredLogger should not lose log messages when an exception is raised") {
    val testLogger = StructuredTestingLogger.impl[IO]()
    DeferredStructuredLogger(testLogger)
      .use { logger =>
        for {
          _ <- logger.info("Test Message")
          _ <- testLogger.logged.assertEquals(Vector.empty)
          _ <- IO.raiseError(new RuntimeException("Expected Exception"))
        } yield ()
      }
      .attempt
      .map(_.leftMap(_.getMessage))
      .assertEquals(Left("Expected Exception"))
      .flatMap(_ => testLogger.logged)
      .assertEquals(Vector(StructuredTestingLogger.INFO("Test Message", none)))
  }

  test("DeferredStructuredLogger should not duplicate log messages when an exception is raised") {
    val testLogger = StructuredTestingLogger.impl[IO]()
    DeferredStructuredLogger(testLogger)
      .use { logger =>
        for {
          _ <- logger.trace("Test Message 0")
          _ <- testLogger.logged.assertEquals(Vector.empty)
          _ <- logger.log
          _ <- testLogger.logged.assertEquals(
            Vector(StructuredTestingLogger.TRACE("Test Message 0", none))
          )
          _ <- logger.info("Test Message 1")
          _ <- IO.raiseError(new RuntimeException("Expected Exception"))
        } yield ()
      }
      .attempt
      .map(_.leftMap(_.getMessage))
      .assertEquals(Left("Expected Exception"))
      .flatMap(_ => testLogger.logged)
      .assertEquals(
        Vector(
          StructuredTestingLogger.TRACE("Test Message 0", none),
          StructuredTestingLogger.INFO("Test Message 1", none)
        )
      )
  }

  test("DeferredStructuredLogger doesn't lose the ability to log when message is modified") {
    val testLogger = StructuredTestingLogger.impl[IO]()
    DeferredStructuredLogger(testLogger)
      .map(_.withModifiedString(_.toUpperCase))
      .use { logger =>
        for {
          _ <- logger.trace("Test Message")
          _ <- testLogger.logged.assertEquals(
            Vector.empty,
            clue("Checking that logging is deferred")
          )
          _ <- logger.log
        } yield ()
      }
      .assertEquals(())
      .flatMap(_ => testLogger.logged)
      .assertEquals(Vector(StructuredTestingLogger.TRACE("TEST MESSAGE", none)))
  }

  test("DeferredStructuredLogger doesn't lose the ability to log when mapK is called") {
    val testLogger = StructuredTestingLogger.impl[IO]()
    DeferredStructuredLogger(testLogger)
      .map(_.mapK[IO](FunctionK.id[IO]))
      .use { logger =>
        for {
          _ <- logger.trace("Test Message")
          _ <- testLogger.logged.assertEquals(
            Vector.empty,
            clue("Checking that logging is deferred")
          )
          _ <- logger.log
        } yield ()
      }
      .assertEquals(())
      .flatMap(_ => testLogger.logged)
      .assertEquals(Vector(StructuredTestingLogger.TRACE("Test Message", none)))
  }

  test("DeferredStructuredLogger doesn't lose the ability to log when context is added") {
    val testLogger = StructuredTestingLogger.impl[IO]()
    val context = List("test" -> "context").toMap
    DeferredStructuredLogger(testLogger)
      .map(_.addContext(context))
      .use { logger =>
        for {
          _ <- logger.trace("Test Message")
          _ <- testLogger.logged.assertEquals(
            Vector.empty,
            clue("Checking that logging is deferred")
          )
          _ <- logger.log
        } yield ()
      }
      .assertEquals(())
      .flatMap(_ => testLogger.logged)
      .assertEquals(Vector(StructuredTestingLogger.TRACE("Test Message", none, context)))
  }
}
