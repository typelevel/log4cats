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
import cats.effect.Ref
import munit.CatsEffectSuite

class SamStructuredLoggerTest extends CatsEffectSuite {

  // Test kernel that captures log calls for verification
  def testKernel[F[_]](
      ref: Ref[F, List[Log[String]]]
  )(implicit F: cats.effect.Sync[F]): LoggerKernel[F, String] = {
    new LoggerKernel[F, String] {
      def log(
          level: KernelLogLevel,
          record: Log.Builder[String] => Log.Builder[String]
      ): F[Unit] = {
        val logRecord = record(Log.mutableBuilder[String]()).build()
        ref.update(_ :+ logRecord)
      }
    }
  }

  test("SamStructuredLogger should handle context with multiple entries") {
    for {
      ref <- Ref.of[IO, List[Log[String]]](List.empty)
      kernel = testKernel[IO](ref)
      logger = SamStructuredLogger.fromKernel(kernel)

      _ <- logger.info(Map("base" -> "value", "extra" -> "data"))("Test message")
      logs <- ref.get
    } yield {
      assertEquals(logs.length, 1)
      val log = logs.head
      assertEquals(log.message(), "Test message")
      assertEquals(log.context.size, 2)
      assertEquals(log.context("base"), "value")
      assertEquals(log.context("extra"), "data")
    }
  }

  test("SamStructuredLogger should support all log levels") {
    for {
      ref <- Ref.of[IO, List[Log[String]]](List.empty)
      kernel = testKernel[IO](ref)
      logger = SamStructuredLogger.fromKernel(kernel)

      _ <- logger.trace(Map("level" -> "trace"))("Trace message")
      _ <- logger.debug(Map("level" -> "debug"))("Debug message")
      _ <- logger.info(Map("level" -> "info"))("Info message")
      _ <- logger.warn(Map("level" -> "warn"))("Warn message")
      _ <- logger.error(Map("level" -> "error"))("Error message")
      logs <- ref.get
    } yield {
      assertEquals(logs.length, 5)
      assertEquals(logs(0).context("level"), "trace")
      assertEquals(logs(1).context("level"), "debug")
      assertEquals(logs(2).context("level"), "info")
      assertEquals(logs(3).context("level"), "warn")
      assertEquals(logs(4).context("level"), "error")
    }
  }

  test("SamStructuredLogger should support error logging with throwables") {
    for {
      ref <- Ref.of[IO, List[Log[String]]](List.empty)
      kernel = testKernel[IO](ref)
      logger = SamStructuredLogger.fromKernel(kernel)

      val exception = new RuntimeException("Test exception")
      _ <- logger.error(Map("error_type" -> "runtime"), exception)("Error with context")
      logs <- ref.get
    } yield {
      assertEquals(logs.length, 1)
      val log = logs.head
      assertEquals(log.message(), "Error with context")
      assertEquals(log.context("error_type"), "runtime")
      assertEquals(log.throwable, Some(exception))
    }
  }

  test("SamStructuredLogger should support addContext functionality") {
    for {
      ref <- Ref.of[IO, List[Log[String]]](List.empty)
      kernel = testKernel[IO](ref)
      logger = SamStructuredLogger.fromKernel(kernel)
      enrichedLogger = logger.addContext(Map("service" -> "test-service"))

      _ <- enrichedLogger.info(Map("request_id" -> "123"))("Request processed")
      logs <- ref.get
    } yield {
      assertEquals(logs.length, 1)
      val log = logs.head
      assertEquals(log.message(), "Request processed")
      assertEquals(log.context("service"), "test-service")
      assertEquals(log.context("request_id"), "123")
    }
  }

  test("SamStructuredLogger should support addContext with key-value pairs") {
    for {
      ref <- Ref.of[IO, List[Log[String]]](List.empty)
      kernel = testKernel[IO](ref)
      logger = SamStructuredLogger.fromKernel(kernel)
      enrichedLogger = logger.addContext("service" -> "test-service", "version" -> "1.0.0")

      _ <- enrichedLogger.info("Service started")
      logs <- ref.get
    } yield {
      assertEquals(logs.length, 1)
      val log = logs.head
      assertEquals(log.message(), "Service started")
      assertEquals(log.context("service"), "test-service")
      assertEquals(log.context("version"), "1.0.0")
    }
  }
}
