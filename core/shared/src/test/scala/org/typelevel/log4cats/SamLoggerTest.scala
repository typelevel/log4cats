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
import munit.CatsEffectSuite

class SamLoggerTest extends CatsEffectSuite {

  test("LoggerKernel should work with a simple console implementation") {
    val kernel = ConsoleLoggerKernel[IO, String]
    val logger = SamLogger.wrap(kernel)

    logger.info("Hello, SAM Logger!").void
  }

  test("LoggerKernel should support structured logging with context") {
    val kernel = ConsoleLoggerKernel[IO, String]
    val logger = SamLogger.wrap(kernel)

    logger
      .info(
        "User action",
        ("user_id", "123"),
        ("action", "login")
      )
      .void
  }

  test("LoggerKernel should support error logging with throwables") {
    val kernel = ConsoleLoggerKernel[IO, String]
    val logger = SamLogger.wrap(kernel)

    val exception = new RuntimeException("Test exception")

    logger
      .error(
        "Something went wrong",
        exception
      )
      .void
  }

  test("LoggerKernel should support multiple log records") {
    val kernel = ConsoleLoggerKernel[IO, String]
    val logger = SamLogger.wrap(kernel)

    logger
      .info(
        "Complex log entry",
        ("request_id", "abc-123"),
        ("duration_ms", 150),
        new RuntimeException("Nested error")
      )
      .void
  }

  test("SamLogger withModifiedString should modify messages correctly") {
    val kernel = ConsoleLoggerKernel[IO, String]
    val logger = SamLogger.wrap(kernel)
    val modifiedLogger = logger.withModifiedString(msg => s"[MODIFIED] $msg")

    // Test that the message is modified
    modifiedLogger.info("Test message").void
  }

  test("SamLogger withModifiedString should preserve context and other fields") {
    val kernel = ConsoleLoggerKernel[IO, String]
    val logger = SamLogger.wrap(kernel)
    val modifiedLogger = logger.withModifiedString(msg => s"[MODIFIED] $msg")

    // Test that context and other fields are preserved
    modifiedLogger
      .info(
        "Test message",
        ("key", "value"),
        new RuntimeException("Test exception")
      )
      .void
  }

  test("LogRecord should combine multiple records correctly") {
    val record1: LogRecord[String] = _.withMessage("Hello")
    val record2: LogRecord[String] = _.withContext("key")("value")

    val combined = LogRecord.combine(Seq(record1, record2))
    val builder = Log.mutableBuilder[String]()
    val result = combined(builder).build()

    assertEquals(result.message(), "Hello")
    assert(result.context.contains("key"))
  }

  test("Recordable should convert strings to log records") {
    val record = implicitly[Recordable[String, String]].record("test message")
    val result = record(Log.mutableBuilder[String]()).build()

    assertEquals(result.message(), "test message")
  }

  test("Recordable should convert tuples to context records") {
    val record = implicitly[Recordable[String, (String, String)]].record(("key", "value"))
    val result = record(Log.mutableBuilder[String]()).build()

    assert(result.context.contains("key"))
  }

  test("Recordable should convert throwables to log records") {
    val exception = new RuntimeException("test")
    val record = implicitly[Recordable[String, Throwable]].record(exception)
    val result = record(Log.mutableBuilder[String]()).build()

    assertEquals(result.throwable, Some(exception))
  }
}
