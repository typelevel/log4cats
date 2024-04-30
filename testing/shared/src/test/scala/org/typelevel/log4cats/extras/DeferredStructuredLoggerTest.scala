package org.typelevel.log4cats.extras

import cats.effect.IO
import cats.syntax.all.*
import org.typelevel.log4cats.testing.StructuredTestingLogger

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
      .assertEquals(())
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
      .assertEquals(())
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
}
