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

class SamLoggerTest extends CatsEffectSuite {

  // Test kernel that captures log calls for verification
  def testKernel[F[_]: cats.effect.Sync](ref: Ref[F, List[Log[String]]]): LoggerKernel[F, String] = {
    new LoggerKernel[F, String] {
      def log(level: KernelLogLevel, record: Log.Builder[String] => Log.Builder[String]): F[Unit] = {
        val logRecord = record(Log.mutableBuilder[String]()).build()
        ref.update(_ :+ logRecord)
      }
    }
  }

  test("SamLogger should execute logging operations and capture them") {
    for {
      ref <- Ref.of[IO, List[Log[String]]](List.empty)
      kernel = testKernel[IO](ref)
      logger = SamLogger.wrap(kernel)
      
      _ <- logger.info("Hello, SAM Logger!")
      logs <- ref.get
    } yield {
      assertEquals(logs.length, 1)
      assertEquals(logs.head.message(), "Hello, SAM Logger!")
    }
  }

  test("SamLogger should support simple message logging") {
    for {
      ref <- Ref.of[IO, List[Log[String]]](List.empty)
      kernel = testKernel[IO](ref)
      logger = SamLogger.wrap(kernel)
      
      _ <- logger.info("User action")
      logs <- ref.get
    } yield {
      assertEquals(logs.length, 1)
      val log = logs.head
      assertEquals(log.message(), "User action")
    }
  }

  test("SamLogger should support error logging") {
    for {
      ref <- Ref.of[IO, List[Log[String]]](List.empty)
      kernel = testKernel[IO](ref)
      logger = SamLogger.wrap(kernel)
      
      _ <- logger.error("Something went wrong")
      logs <- ref.get
    } yield {
      assertEquals(logs.length, 1)
      val log = logs.head
      assertEquals(log.message(), "Something went wrong")
    }
  }

  test("SamLogger should support multiple log calls") {
    for {
      ref <- Ref.of[IO, List[Log[String]]](List.empty)
      kernel = testKernel[IO](ref)
      logger = SamLogger.wrap(kernel)
      
      _ <- logger.info("First log entry")
      _ <- logger.info("Second log entry")
      logs <- ref.get
    } yield {
      assertEquals(logs.length, 2)
      assertEquals(logs(0).message(), "First log entry")
      assertEquals(logs(1).message(), "Second log entry")
    }
  }

  test("SamLogger withModifiedString should modify messages correctly") {
    for {
      ref <- Ref.of[IO, List[Log[String]]](List.empty)
      kernel = testKernel[IO](ref)
      logger = SamLogger.wrap(kernel)
      modifiedLogger = logger.withModifiedString(msg => s"[MODIFIED] $msg")
      
      _ <- modifiedLogger.info("Test message")
      logs <- ref.get
    } yield {
      assertEquals(logs.length, 1)
      assertEquals(logs.head.message(), "[MODIFIED] Test message")
    }
  }

  test("SamLogger withModifiedString should preserve other fields") {
    for {
      ref <- Ref.of[IO, List[Log[String]]](List.empty)
      kernel = testKernel[IO](ref)
      logger = SamLogger.wrap(kernel)
      modifiedLogger = logger.withModifiedString(msg => s"[MODIFIED] $msg")
      
      _ <- modifiedLogger.info("Test message")
      logs <- ref.get
    } yield {
      assertEquals(logs.length, 1)
      val log = logs.head
      assertEquals(log.message(), "[MODIFIED] Test message")
    }
  }

}
