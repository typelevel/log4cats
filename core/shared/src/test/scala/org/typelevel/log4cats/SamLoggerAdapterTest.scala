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

class SamLoggerAdapterTest extends CatsEffectSuite {

  test("Adapter should convert LoggerKernel to Logger") {
    val kernel = ConsoleLoggerKernel[IO, String]
    val logger = SamLoggerAdapter.toLogger(kernel)

    // Test basic logging
    logger.info("Test message").void
    logger.error("Test error").void
    logger.warn("Test warning").void
    logger.debug("Test debug").void
    logger.trace("Test trace").void
  }

  test("Adapter should convert Logger to LoggerKernel") {
    val originalKernel = ConsoleLoggerKernel[IO, String]
    val logger = SamLoggerAdapter.toLogger(originalKernel)
    val kernel = SamLoggerAdapter.toLoggerKernel(logger)

    // Test that the kernel works
    kernel.log(KernelLogLevel.Info, _.withMessage("Test via adapter")).void
  }

  test("Adapter should handle throwables correctly") {
    val kernel = ConsoleLoggerKernel[IO, String]
    val logger = SamLoggerAdapter.toLogger(kernel)
    val exception = new RuntimeException("Test exception")

    logger.error(exception)("Error with throwable").void
    logger.warn(exception)("Warning with throwable").void
    logger.info(exception)("Info with throwable").void
    logger.debug(exception)("Debug with throwable").void
    logger.trace(exception)("Trace with throwable").void
  }

  test("Adapter should convert SamLogger to Logger") {
    val kernel = ConsoleLoggerKernel[IO, String]
    val samLogger = SamLogger.wrap(kernel)
    val logger = SamLoggerAdapter.samLoggerToLogger(samLogger)

    logger.info("Test from SamLogger").void
    logger.error("Test error from SamLogger").void
  }

  test("Adapter should convert Logger to SamLogger") {
    val kernel = ConsoleLoggerKernel[IO, String]
    val logger = SamLoggerAdapter.toLogger(kernel)
    val samLogger = SamLoggerAdapter.loggerToSamLogger[IO, String](logger)

    // Test that the SamLogger works with structured logging
    samLogger
      .info(
        "Test structured logging",
        ("key1", "value1"),
        ("key2", "value2")
      )
      .void
  }

  test("Round-trip conversion should preserve functionality") {
    val originalKernel = ConsoleLoggerKernel[IO, String]
    val logger = SamLoggerAdapter.toLogger(originalKernel)
    val roundTripKernel = SamLoggerAdapter.toLoggerKernel(logger)

    // Test that round-trip conversion works
    roundTripKernel.log(KernelLogLevel.Info, _.withMessage("Round-trip test")).void
  }
}
