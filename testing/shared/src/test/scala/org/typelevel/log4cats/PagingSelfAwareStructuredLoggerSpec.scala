/*
 * Copyright 2018 Christopher Davenport
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
import cats.effect.unsafe.implicits.global
import ch.qos.logback.classic.{Level, Logger}
import org.slf4j.LoggerFactory
import org.specs2.mutable.Specification
import org.specs2.specification.BeforeAfterAll
import org.typelevel.log4cats.testing.StructuredTestingLogger

/**
 * This test class runs the test cases with StructuredTestingLogger
 */
class PagingSelfAwareStructuredLoggerSpec extends Specification with BeforeAfterAll {

  private var origLogLevel = Level.OFF
  private val rootLogger =
    LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).asInstanceOf[Logger]

  private val ctx = Map("foo" -> "bar")
  private val msg = "0123456789abcdef" * 128 // Size of message is 2K byte
  private val excptn = new RuntimeException(
    "Nothing wrong here, this exception is used for testing"
  )
  private val uuidPatternRegex =
    "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"

  def beforeAll(): Unit = {
    origLogLevel = rootLogger.getLevel
    rootLogger.setLevel(Level.INFO)
  }

  def afterAll(): Unit = rootLogger.setLevel(origLogLevel)

  def runTest(
      pageSizeK: Int,
      maxPageNeeded: Int,
      expectedNumOfPage: Int,
      logLevel: String,
      logTest: SelfAwareStructuredLogger[IO] => IO[Unit],
      suiteName: String = "",
      caseName: String = ""
  ): Either[Throwable, Vector[StructuredTestingLogger.LogMessage]] = {
    val pageSize = pageSizeK * 1024
    val stl = StructuredTestingLogger.impl[IO]()
    val pagingStl: SelfAwareStructuredLogger[IO] =
      PagingSelfAwareStructuredLogger.withPaging[IO](pageSizeK, maxPageNeeded)(stl)
    val logResult: IO[Unit] = logTest(pagingStl)

    val test = logResult >> stl.logged.attempt
    val logged = test.unsafeRunSync()

    logged.fold(
      _ => anError,
      (loggedVec: Vector[StructuredTestingLogger.LogMessage]) => {
        // Below it will assert the expectedNumOfPage, logging context and logged contents
        // If an assertion does not pass, print out relevant info.

        if (loggedVec.size != expectedNumOfPage) {
          println(s"\nFailed: $suiteName - $caseName - $logLevel")
          println("Number of log entry does not match expectation")
          println(s"loggedVec.size=${loggedVec.size}, expectedNumOfPage=$expectedNumOfPage")
          println(s"loggedVec=$loggedVec")
        }
        loggedVec.size must_== expectedNumOfPage

        val loggedVecWithIndex = loggedVec.zip(1 to loggedVec.size)
        val allMsgValid = loggedVecWithIndex.forall { mi =>
          val (logMsg, pageNum) = mi
          val ctxValid =
            logMsg.ctx.getOrElse("log_split_id", "").matches(uuidPatternRegex) &&
              (logMsg.ctx.getOrElse("page_size", "0 Kib").dropRight(4).toInt == pageSizeK) &&
              (logMsg.ctx.getOrElse("log_size", "0 Byte").dropRight(5).toInt > pageSize || expectedNumOfPage == 1)
          if (!ctxValid) {
            println(s"\nFailed: $suiteName - $caseName - $logLevel")
            println("Logging context does not match expectation")
            println(s"pageNum=$pageNum, logMsg.ctx=${logMsg.ctx}")
          }

          val msgValid = expectedNumOfPage == 1 ||
            (pageNum == loggedVec.size || logMsg.message.length > pageSize) &&
            logMsg.message.endsWith(s" page_size=$pageSizeK Kib") &&
            logMsg.message.startsWith("Page ")
          if (!msgValid) {
            println(s"\nFailed: $suiteName - $caseName - $logLevel")
            println("Logged message page does not match expectation")
            println(s"pageNum=$pageNum, logMsg=$logMsg")
          }

          ctxValid && msgValid
        }
        allMsgValid must_== true
      }
    )

    logged
  }

  val singlePageSuite1 =
    "PagingSelfAwareStructuredLogger logs a message of 2 KB size in 1 log entry when page size is 3 KB"

  singlePageSuite1 should {
    val case1 = "Log at trace level"
    case1 in {
      runTest(3, 10, 1, "trace", _.trace(msg), singlePageSuite1, case1) must beRight
    }

    val case2 = "Log at debug level"
    case2 in {
      runTest(3, 10, 1, "debug", _.debug(msg), singlePageSuite1, case2) must beRight
    }

    val case3 = "Log at info level"
    case3 in {
      runTest(3, 10, 1, "info", _.info(msg), singlePageSuite1, case3) must beRight
    }

    val case4 = "Log at warn level"
    case4 in {
      runTest(3, 10, 1, "warn", _.warn(msg), singlePageSuite1, case4) must beRight
    }

    val case5 = "Log at error level"
    case5 in {
      runTest(3, 10, 1, "error", _.error(msg), singlePageSuite1, case5) must beRight
    }
  }

  val singlePageSuite2 =
    "PagingSelfAwareStructuredLogger logs a message of 2 KB size and an exception in 1 log entry when page size is 10 KB"

  singlePageSuite2 should {
    val case1 = "Log at trace level"
    case1 in {
      runTest(10, 10, 1, "trace", _.trace(excptn)(msg), singlePageSuite2, case1) must beRight
    }

    val case2 = "Log at debug level"
    case2 in {
      runTest(10, 10, 1, "debug", _.debug(excptn)(msg), singlePageSuite2, case2) must beRight
    }

    val case3 = "Log at info level"
    case3 in {
      runTest(10, 10, 1, "info", _.info(excptn)(msg), singlePageSuite2, case3) must beRight
    }

    val case4 = "Log at warn level"
    case4 in {
      runTest(10, 10, 1, "warn", _.warn(excptn)(msg), singlePageSuite2, case4) must beRight
    }

    val case5 = "Log at error level"
    case5 in {
      runTest(10, 10, 1, "error", _.error(excptn)(msg), singlePageSuite2, case5) must beRight
    }
  }

  val singlePageSuite3 =
    "PagingSelfAwareStructuredLogger logs a message of 2 KB size with context in 1 log entry when page size is 3 KB"

  singlePageSuite3 should {
    val case1 = "Log at trace level"
    case1 in {
      runTest(3, 10, 1, "trace", _.trace(ctx)(msg), singlePageSuite3, case1) must beRight
    }

    val case2 = "Log at debug level"
    case2 in {
      runTest(3, 10, 1, "debug", _.debug(ctx)(msg), singlePageSuite3, case2) must beRight
    }

    val case3 = "Log at info level"
    case3 in {
      runTest(3, 10, 1, "info", _.info(ctx)(msg), singlePageSuite3, case3) must beRight
    }

    val case4 = "Log at warn level"
    case4 in {
      runTest(3, 10, 1, "warn", _.warn(ctx)(msg), singlePageSuite3, case4) must beRight
    }

    val case5 = "Log at error level"
    case5 in {
      runTest(3, 10, 1, "error", _.error(ctx)(msg), singlePageSuite3, case5) must beRight
    }
  }

  val singlePageSuite4 =
    "PagingSelfAwareStructuredLogger logs a message of 2 KB size with context and an exception in 1 log entry when page size is 10 KB"

  singlePageSuite4 should {
    val case1 = "Log at trace level"
    case1 in {
      runTest(10, 10, 1, "trace", _.trace(ctx, excptn)(msg), singlePageSuite4, case1) must beRight
    }

    val case2 = "Log at debug level"
    case2 in {
      runTest(10, 10, 1, "debug", _.debug(ctx, excptn)(msg), singlePageSuite4, case2) must beRight
    }

    val case3 = "Log at info level"
    case3 in {
      runTest(10, 10, 1, "info", _.info(ctx, excptn)(msg), singlePageSuite4, case3) must beRight
    }

    val case4 = "Log at warn level"
    case4 in {
      runTest(10, 10, 1, "warn", _.warn(ctx, excptn)(msg), singlePageSuite4, case4) must beRight
    }

    val case5 = "Log at error level"
    case5 in {
      runTest(10, 10, 1, "error", _.error(ctx, excptn)(msg), singlePageSuite4, case5) must beRight
    }
  }

  val multiplePageSuite1 =
    "PagingSelfAwareStructuredLogger logs a message of 2 KB size in 2 log entries when page size is 1 KB"

  multiplePageSuite1 should {
    val case1 = "Log at trace level"
    case1 in {
      runTest(1, 10, 2, "trace", _.trace(msg), multiplePageSuite1, case1) must beRight
    }

    val case2 = "Log at debug level"
    case2 in {
      runTest(1, 10, 2, "debug", _.debug(msg), multiplePageSuite1, case2) must beRight
    }

    val case3 = "Log at info level"
    case3 in {
      runTest(1, 10, 2, "info", _.info(msg), multiplePageSuite1, case3) must beRight
    }

    val case4 = "Log at warn level"
    case4 in {
      runTest(1, 10, 2, "warn", _.warn(msg), multiplePageSuite1, case4) must beRight
    }

    val case5 = "Log at error level"
    case5 in {
      runTest(1, 10, 2, "error", _.error(msg), multiplePageSuite1, case5) must beRight
    }
  }

  val multiplePageSuite2 =
    "PagingSelfAwareStructuredLogger logs a message of 2 KB size and an exception in 3 log entries when page size is 3 KB"

  multiplePageSuite2 should {
    val case1 = "Log at trace level"
    case1 in {
      runTest(3, 10, 3, "trace", _.trace(excptn)(msg), multiplePageSuite2, case1) must beRight
    }

    val case2 = "Log at debug level"
    case2 in {
      runTest(3, 10, 3, "debug", _.debug(excptn)(msg), multiplePageSuite2, case2) must beRight
    }

    val case3 = "Log at info level"
    case3 in {
      runTest(3, 10, 3, "info", _.info(excptn)(msg), multiplePageSuite2, case3) must beRight
    }

    val case4 = "Log at warn level"
    case4 in {
      runTest(3, 10, 3, "warn", _.warn(excptn)(msg), multiplePageSuite2, case4) must beRight
    }

    val case5 = "Log at error level"
    case5 in {
      runTest(3, 10, 3, "error", _.error(excptn)(msg), multiplePageSuite2, case5) must beRight
    }
  }

  val multiplePageSuite3 =
    "PagingSelfAwareStructuredLogger logs a message of 2 KB size with context in 2 log entries when page size is 1 KB"

  multiplePageSuite3 should {
    val case1 = "Log at trace level"
    case1 in {
      runTest(1, 10, 2, "trace", _.trace(ctx)(msg), multiplePageSuite3, case1) must beRight
    }

    val case2 = "Log at debug level"
    case2 in {
      runTest(1, 10, 2, "debug", _.debug(ctx)(msg), multiplePageSuite3, case2) must beRight
    }

    val case3 = "Log at info level"
    case3 in {
      runTest(1, 10, 2, "info", _.info(ctx)(msg), multiplePageSuite3, case3) must beRight
    }

    val case4 = "Log at warn level"
    case4 in {
      runTest(1, 10, 2, "warn", _.warn(ctx)(msg), multiplePageSuite3, case4) must beRight
    }

    val case5 = "Log at error level"
    case5 in {
      runTest(1, 10, 2, "error", _.error(ctx)(msg), multiplePageSuite3, case5) must beRight
    }
  }

  val multiplePageSuite4 =
    "PagingSelfAwareStructuredLogger logs a message of 2 KB size with context and an exception in 3 log entries when page size is 3 KB"

  multiplePageSuite4 should {
    val case1 = "Log at trace level"
    case1 in {
      runTest(3, 10, 3, "trace", _.trace(ctx, excptn)(msg), multiplePageSuite4, case1) must beRight
    }

    val case2 = "Log at debug level"
    case2 in {
      runTest(3, 10, 3, "debug", _.debug(ctx, excptn)(msg), multiplePageSuite4, case2) must beRight
    }

    val case3 = "Log at info level"
    case3 in {
      runTest(3, 10, 3, "info", _.info(ctx, excptn)(msg), multiplePageSuite4, case3) must beRight
    }

    val case4 = "Log at warn level"
    case4 in {
      runTest(3, 10, 3, "warn", _.warn(ctx, excptn)(msg), multiplePageSuite4, case4) must beRight
    }

    val case5 = "Log at error level"
    case5 in {
      runTest(3, 10, 3, "error", _.error(ctx, excptn)(msg), multiplePageSuite4, case5) must beRight
    }
  }

  val maxPageNumSuite =
    "PagingSelfAwareStructuredLogger with maxPageNeeded = 2 logs a message of 2 KB size with context and an exception in 2 log entries when page size is 3 KB"

  maxPageNumSuite should {
    val case1 = "Log at trace level"
    case1 in {
      runTest(3, 2, 2, "trace", _.trace(ctx, excptn)(msg), maxPageNumSuite, case1) must beRight
    }

    val case2 = "Log at debug level"
    case2 in {
      runTest(3, 2, 2, "debug", _.debug(ctx, excptn)(msg), maxPageNumSuite, case2) must beRight
    }

    val case3 = "Log at info level"
    case3 in {
      runTest(3, 2, 2, "info", _.info(ctx, excptn)(msg), maxPageNumSuite, case3) must beRight
    }

    val case4 = "Log at warn level"
    case4 in {
      runTest(3, 2, 2, "warn", _.warn(ctx, excptn)(msg), maxPageNumSuite, case4) must beRight
    }

    val case5 = "Log at error level"
    case5 in {
      runTest(3, 2, 2, "error", _.error(ctx, excptn)(msg), maxPageNumSuite, case5) must beRight
    }
  }

  val parameterChkSuite =
    "PagingSelfAwareStructuredLogger.withPaging() parameters pageSizeK and maxPageNeeded"

  parameterChkSuite should {
    val case1 = "Throw AssertionError for negative pageSizeK"
    case1 in {
      runTest(-3, 2, 2, "trace", _.trace(msg), parameterChkSuite, case1) must throwAn[
        IllegalArgumentException
      ]
    }

    val case2 = "Throw AssertionError for negative maxPageNeeded"
    case2 in {
      runTest(3, -2, 2, "debug", _.debug(excptn)(msg), parameterChkSuite, case2) must throwAn[
        IllegalArgumentException
      ]
    }

    val case3 = "Throw AssertionError for pageSizeK=0"
    case3 in {
      runTest(0, 2, 2, "trace", _.info(ctx)(msg), parameterChkSuite, case3) must throwAn[
        IllegalArgumentException
      ]
    }

    val case4 = "Throw AssertionError for maxPageNeeded=0"
    case4 in {
      runTest(3, 0, 2, "debug", _.warn(ctx, excptn)(msg), parameterChkSuite, case4) must throwAn[
        IllegalArgumentException
      ]
    }

    val case5 = "Throw AssertionError for pageSizeK=0, maxPageNeeded=0"
    case5 in {
      runTest(0, 0, 2, "debug", _.warn(ctx, excptn)(msg), parameterChkSuite, case5) must throwAn[
        IllegalArgumentException
      ]
    }
  }
}
