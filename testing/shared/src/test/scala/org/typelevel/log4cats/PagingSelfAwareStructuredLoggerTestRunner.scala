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
import org.typelevel.log4cats.testing.StructuredTestingLogger

/**
 * This test class runs the test cases with StructuredTestingLogger
 */
class PagingSelfAwareStructuredLoggerTestRunner extends CatsEffectSuite {

  val msg: String = "0123456789abcdef" * 128 // Size of message is 2K byte
  val ctx: Map[String, String] = Map("foo" -> "bar")
  val excptn: RuntimeException = new RuntimeException(
    "Nothing wrong here, this exception is used for testing"
  )
  val uuidPatternRegex =
    "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"

  def runTest(
      pageSizeK: Int,
      maxPageNeeded: Int,
      expectedNumOfPage: Int,
      logLevel: String,
      logTest: SelfAwareStructuredLogger[IO] => IO[Unit],
      suiteCaseName: String = ""
  ): IO[Unit] = {
    val pageSize = pageSizeK * 1024
    val stl = StructuredTestingLogger.impl[IO]()
    val pagingStl: SelfAwareStructuredLogger[IO] =
      PagingSelfAwareStructuredLogger.withPaging[IO](pageSizeK, maxPageNeeded)(stl)
    val logResult: IO[Unit] = logTest(pagingStl)

    val test = logResult >> stl.logged
    test.redeem(
      _ => fail("Failed to log"),
      (loggedVec: Vector[StructuredTestingLogger.LogMessage]) => {
        // Below it will assert the expectedNumOfPage, logging context and logged contents
        // If an assertion does not pass, print out relevant info.

        if (loggedVec.size != expectedNumOfPage) {
          println(s"\nFailed: $suiteCaseName - $logLevel")
          println("Number of log entry does not match expectation")
          println(s"loggedVec.size=${loggedVec.size}, expectedNumOfPage=$expectedNumOfPage")
          println(s"loggedVec=$loggedVec")
        }
        assertEquals(loggedVec.size, expectedNumOfPage)

        val loggedVecWithIndex = loggedVec.zip(1 to loggedVec.size)
        val allMsgValid = loggedVecWithIndex.forall { mi =>
          val (logMsg, pageNum) = mi
          val ctxValid =
            logMsg.ctx.getOrElse("log_split_id", "").matches(uuidPatternRegex) &&
              (logMsg.ctx.getOrElse("page_size", "0 Kib").dropRight(4).toInt == pageSizeK) &&
              (logMsg.ctx
                .getOrElse("log_size", "0 Byte")
                .dropRight(5)
                .toInt > pageSize || expectedNumOfPage == 1)
          if (!ctxValid) {
            println(s"\nFailed: $suiteCaseName - $logLevel")
            println("Logging context does not match expectation")
            println(s"pageNum=$pageNum, logMsg.ctx=${logMsg.ctx}")
          }

          val msgValid = expectedNumOfPage == 1 ||
            (pageNum == loggedVec.size || logMsg.message.length > pageSize) &&
            logMsg.message.endsWith(s" page_size=$pageSizeK Kib") &&
            logMsg.message.startsWith("Page ")
          if (!msgValid) {
            println(s"\nFailed: $suiteCaseName - $logLevel")
            println("Logged message page does not match expectation")
            println(s"pageNum=$pageNum, logMsg=$logMsg")
          }

          ctxValid && msgValid
        }
        assertEquals(allMsgValid, true)
      }
    )
  }

  def runTestExpectingThrow(
      pageSizeK: Int,
      maxPageNeeded: Int,
      suiteName: String = "",
      caseName: String = ""
  ): Unit = {
    val stl = StructuredTestingLogger.impl[IO]()
    try {
      PagingSelfAwareStructuredLogger.withPaging[IO](pageSizeK, maxPageNeeded)(stl)
      fail(s"$suiteName $caseName: Expected exception not thrown")
    } catch {
      case thr: Throwable => assert(thr.isInstanceOf[IllegalArgumentException])
    }
  }

}
