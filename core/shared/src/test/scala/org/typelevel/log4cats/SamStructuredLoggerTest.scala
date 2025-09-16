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

class SamStructuredLoggerTest extends CatsEffectSuite {

  test("SamStructuredLogger should handle context with multiple entries") {
    var capturedLogs: List[(KernelLogLevel, Log.Builder[String] => Log.Builder[String])] = Nil

    val testKernel = new LoggerKernel[IO, String] {
      def log(
          level: KernelLogLevel,
          record: Log.Builder[String] => Log.Builder[String]
      ): IO[Unit] = {
        capturedLogs = capturedLogs :+ (level, record)
        IO.unit
      }
    }

    val logger = SamStructuredLogger.fromKernel(testKernel)
    logger.info(Map("base" -> "value", "extra" -> "data"))("Test message").void

    assertEquals(capturedLogs.length, 1)
    val (level, record) = capturedLogs.head
    assertEquals(level, KernelLogLevel.Info)

    val logRecord = record(Log.mutableBuilder[String]()).build()
    assertEquals(logRecord.message(), "Test message")
    assertEquals(logRecord.context.size, 2)
    assertEquals(logRecord.context("base"), "value")
    assertEquals(logRecord.context("extra"), "data")
  }
}
