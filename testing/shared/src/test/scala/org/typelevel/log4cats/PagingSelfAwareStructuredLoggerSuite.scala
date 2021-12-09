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

/**
 * This test class runs the test cases with StructuredTestingLogger
 */
class PagingSelfAwareStructuredLoggerSuite extends PagingSelfAwareStructuredLoggerTestRunner {

  val singlePageSuite1 =
    "PagingSelfAwareStructuredLogger logs a message of 2 KB size in 1 log entry when page size is 3 KB"

  val suiteCase_s11 = "singlePageSuite1: Log at trace level"
  test(suiteCase_s11) {
    runTest(3, 10, 1, "trace", _.trace(msg), suiteCase_s11)
  }

  val suiteCase_s12 = "singlePageSuite1: Log at debug level"
  test(suiteCase_s12) {
    runTest(3, 10, 1, "debug", _.debug(msg), suiteCase_s12)
  }

  val suiteCase_s13 = "singlePageSuite1: Log at info level"
  test(suiteCase_s13) {
    runTest(3, 10, 1, "info", _.info(msg), suiteCase_s13)
  }

  val suiteCase_s14 = "singlePageSuite1: Log at warn level"
  test(suiteCase_s14) {
    runTest(3, 10, 1, "warn", _.warn(msg), suiteCase_s14)
  }

  val suiteCase_s15 = "singlePageSuite1: Log at error level"
  test(suiteCase_s15) {
    runTest(3, 10, 1, "error", _.error(msg), suiteCase_s15)
  }

  val singlePageSuite2 =
    "PagingSelfAwareStructuredLogger logs a message of 2 KB size and an exception in 1 log entry when page size is 10 KB"

  val suiteCase_s21 = "singlePageSuite2: Log at trace level"
  test(suiteCase_s21) {
    runTest(10, 10, 1, "trace", _.trace(excptn)(msg), suiteCase_s21)
  }

  val suiteCase_s22 = "singlePageSuite2: Log at debug level"
  test(suiteCase_s22) {
    runTest(10, 10, 1, "debug", _.debug(excptn)(msg), suiteCase_s22)
  }

  val suiteCase_s23 = "singlePageSuite2: Log at info level"
  test(suiteCase_s23) {
    runTest(10, 10, 1, "info", _.info(excptn)(msg), suiteCase_s23)
  }

  val suiteCase_s24 = "singlePageSuite2: Log at warn level"
  test(suiteCase_s24) {
    runTest(10, 10, 1, "warn", _.warn(excptn)(msg), suiteCase_s24)
  }

  val suiteCase_s25 = "singlePageSuite2: Log at error level"
  test(suiteCase_s25) {
    runTest(10, 10, 1, "error", _.error(excptn)(msg), suiteCase_s25)
  }

  val singlePageSuite3 =
    "PagingSelfAwareStructuredLogger logs a message of 2 KB size with context in 1 log entry when page size is 3 KB"

  val suiteCase_s31 = "singlePageSuite3: Log at trace level"
  test(suiteCase_s31) {
    runTest(3, 10, 1, "trace", _.trace(ctx)(msg), suiteCase_s31)
  }

  val suiteCase_s32 = "singlePageSuite3: Log at debug level"
  test(suiteCase_s32) {
    runTest(3, 10, 1, "debug", _.debug(ctx)(msg), suiteCase_s32)
  }

  val suiteCase_s33 = "singlePageSuite3: Log at info level"
  test(suiteCase_s33) {
    runTest(3, 10, 1, "info", _.info(ctx)(msg), suiteCase_s33)
  }

  val suiteCase_s34 = "singlePageSuite3: Log at warn level"
  test(suiteCase_s34) {
    runTest(3, 10, 1, "warn", _.warn(ctx)(msg), suiteCase_s34)
  }

  val suiteCase_s35 = "singlePageSuite3: Log at error level"
  test(suiteCase_s35) {
    runTest(3, 10, 1, "error", _.error(ctx)(msg), suiteCase_s35)
  }

  val singlePageSuite4 =
    "PagingSelfAwareStructuredLogger logs a message of 2 KB size with context and an exception in 1 log entry when page size is 10 KB"

  val suiteCase_s41 = "singlePageSuite4: Log at trace level"
  test(suiteCase_s41) {
    runTest(10, 10, 1, "trace", _.trace(ctx, excptn)(msg), suiteCase_s41)
  }

  val suiteCase_s42 = "singlePageSuite4: Log at debug level"
  test(suiteCase_s42) {
    runTest(10, 10, 1, "debug", _.debug(ctx, excptn)(msg), suiteCase_s42)
  }

  val suiteCase_s43 = "singlePageSuite4: Log at info level"
  test(suiteCase_s43) {
    runTest(10, 10, 1, "info", _.info(ctx, excptn)(msg), suiteCase_s43)
  }

  val suiteCase_s44 = "singlePageSuite4: Log at warn level"
  test(suiteCase_s44) {
    runTest(10, 10, 1, "warn", _.warn(ctx, excptn)(msg), suiteCase_s44)
  }

  val suiteCase_s45 = "singlePageSuite4: Log at error level"
  test(suiteCase_s45) {
    runTest(10, 10, 1, "error", _.error(ctx, excptn)(msg), suiteCase_s45)
  }

  val multiplePageSuite1 =
    "PagingSelfAwareStructuredLogger logs a message of 2 KB size in 2 log entries when page size is 1 KB"

  val suiteCase_m11 = "multiplePageSuite1: Log at trace level"
  test(suiteCase_m11) {
    runTest(1, 10, 2, "trace", _.trace(msg), suiteCase_m11)
  }

  val suiteCase_m12 = "multiplePageSuite1: Log at debug level"
  test(suiteCase_m12) {
    runTest(1, 10, 2, "debug", _.debug(msg), suiteCase_m12)
  }

  val suiteCase_m13 = "multiplePageSuite1: Log at info level"
  test(suiteCase_m13) {
    runTest(1, 10, 2, "info", _.info(msg), suiteCase_m13)
  }

  val suiteCase_m14 = "multiplePageSuite1: Log at warn level"
  test(suiteCase_m14) {
    runTest(1, 10, 2, "warn", _.warn(msg), suiteCase_m14)
  }

  val suiteCase_m15 = "multiplePageSuite1: Log at error level"
  test(suiteCase_m15) {
    runTest(1, 10, 2, "error", _.error(msg), suiteCase_m15)
  }

  val multiplePageSuite2 =
    "PagingSelfAwareStructuredLogger logs a message of 2 KB size and an exception in 2 log entries when page size is 3 KB"

  val suiteCase_m21 = "multiplePageSuite2: Log at trace level"
  test(suiteCase_m21) {
    runTest(3, 10, 2, "trace", _.trace(excptn)(msg), suiteCase_m21)
  }

  val suiteCase_m22 = "multiplePageSuite2: Log at debug level"
  test(suiteCase_m22) {
    runTest(3, 10, 2, "debug", _.debug(excptn)(msg), suiteCase_m22)
  }

  val suiteCase_m23 = "multiplePageSuite2: Log at info level"
  test(suiteCase_m23) {
    runTest(3, 10, 2, "info", _.info(excptn)(msg), suiteCase_m23)
  }

  val suiteCase_m24 = "multiplePageSuite2: Log at warn level"
  test(suiteCase_m24) {
    runTest(3, 10, 2, "warn", _.warn(excptn)(msg), suiteCase_m24)
  }

  val suiteCase_m25 = "multiplePageSuite2: Log at error level"
  test(suiteCase_m25) {
    runTest(3, 10, 2, "error", _.error(excptn)(msg), suiteCase_m25)
  }

  val multiplePageSuite3 =
    "PagingSelfAwareStructuredLogger logs a message of 2 KB size with context in 2 log entries when page size is 1 KB"

  val suiteCase_m31 = "multiplePageSuite3: Log at trace level"
  test(suiteCase_m31) {
    runTest(1, 10, 2, "trace", _.trace(ctx)(msg), suiteCase_m31)
  }

  val suiteCase_m32 = "multiplePageSuite3: Log at debug level"
  test(suiteCase_m32) {
    runTest(1, 10, 2, "debug", _.debug(ctx)(msg), suiteCase_m32)
  }

  val suiteCase_m33 = "multiplePageSuite3: Log at info level"
  test(suiteCase_m33) {
    runTest(1, 10, 2, "info", _.info(ctx)(msg), suiteCase_m33)
  }

  val suiteCase_m34 = "multiplePageSuite3: Log at warn level"
  test(suiteCase_m34) {
    runTest(1, 10, 2, "warn", _.warn(ctx)(msg), suiteCase_m34)
  }

  val suiteCase_m35 = "multiplePageSuite3: Log at error level"
  test(suiteCase_m35) {
    runTest(1, 10, 2, "error", _.error(ctx)(msg), suiteCase_m35)
  }

  val multiplePageSuite4 =
    "PagingSelfAwareStructuredLogger logs a message of 2 KB size with context and an exception in 3 log entries when page size is 3 KB"

  val suiteCase_m41 = "multiplePageSuite4: Log at trace level"
  test(suiteCase_m41) {
    runTest(3, 10, 2, "trace", _.trace(ctx, excptn)(msg), suiteCase_m41)
  }

  val suiteCase_m42 = "multiplePageSuite4: Log at debug level"
  test(suiteCase_m42) {
    runTest(3, 10, 2, "debug", _.debug(ctx, excptn)(msg), suiteCase_m42)
  }

  val suiteCase_m43 = "multiplePageSuite4: Log at info level"
  test(suiteCase_m43) {
    runTest(3, 10, 2, "info", _.info(ctx, excptn)(msg), suiteCase_m43)
  }

  val suiteCase_m44 = "multiplePageSuite4: Log at warn level"
  test(suiteCase_m44) {
    runTest(3, 10, 2, "warn", _.warn(ctx, excptn)(msg), suiteCase_m44)
  }

  val suiteCase_m45 = "multiplePageSuite4: Log at error level"
  test(suiteCase_m45) {
    runTest(3, 10, 2, "error", _.error(ctx, excptn)(msg), suiteCase_m45)
  }

  val maxPageNumSuite =
    "PagingSelfAwareStructuredLogger with maxPageNeeded = 2 logs a message of 2 KB size with context and an exception in 2 log entries when page size is 3 KB"

  val suiteCase_mp1 = "maxPageNumSuite: Log at trace level"
  test(suiteCase_mp1) {
    runTest(3, 2, 2, "trace", _.trace(ctx, excptn)(msg), suiteCase_mp1)
  }

  val suiteCase_mp2 = "maxPageNumSuite: Log at debug level"
  test(suiteCase_mp2) {
    runTest(3, 2, 2, "debug", _.debug(ctx, excptn)(msg), suiteCase_mp2)
  }

  val suiteCase_mp3 = "maxPageNumSuite: Log at info level"
  test(suiteCase_mp3) {
    runTest(3, 2, 2, "info", _.info(ctx, excptn)(msg), suiteCase_mp3)
  }

  val suiteCase_mp4 = "maxPageNumSuite: Log at warn level"
  test(suiteCase_mp4) {
    runTest(3, 2, 2, "warn", _.warn(ctx, excptn)(msg), suiteCase_mp4)
  }

  val suiteCase_mp5 = "maxPageNumSuite: Log at error level"
  test(suiteCase_mp5) {
    runTest(3, 2, 2, "error", _.error(ctx, excptn)(msg), suiteCase_mp5)
  }

  val parameterChkSuite =
    "PagingSelfAwareStructuredLogger.withPaging() parameters pageSizeK and maxPageNeeded"

  val suiteCase_pc1 = "parameterChkSuite: Throw IllegalArgumentException for negative pageSizeK"
  test(suiteCase_pc1) {
    runTestExpectingThrow(-3, 2, parameterChkSuite, suiteCase_pc1)
  }

  val suiteCase_pc2 = "parameterChkSuite: Throw IllegalArgumentException for negative maxPageNeeded"
  test(suiteCase_pc2) {
    runTestExpectingThrow(3, -2, parameterChkSuite, suiteCase_pc2)
  }

  val suiteCase_pc3 = "parameterChkSuite: Throw IllegalArgumentException for pageSizeK=0"
  test(suiteCase_pc3) {
    runTestExpectingThrow(0, 2, parameterChkSuite, suiteCase_pc3)
  }

  val suiteCase_pc4 = "parameterChkSuite: Throw IllegalArgumentException for maxPageNeeded=0"
  test(suiteCase_pc4) {
    runTestExpectingThrow(3, 0, parameterChkSuite, suiteCase_pc4)
  }

  val suiteCase_pc5 = "parameterChkSuite: Throw IllegalArgumentException for pageSizeK=0, maxPageNeeded=0"
  test(suiteCase_pc5) {
    runTestExpectingThrow(0, 0, parameterChkSuite, suiteCase_pc5)
  }
}
