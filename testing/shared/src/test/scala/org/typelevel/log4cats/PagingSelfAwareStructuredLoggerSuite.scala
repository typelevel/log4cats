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

  val case_s11 = "singlePageSuite1: Log at trace level"
  test(case_s11) {
    runTest(3, 10, 1, "trace", _.trace(msg), singlePageSuite1, case_s11)
  }

  val case_s12 = "singlePageSuite1: Log at debug level"
  test(case_s12) {
    runTest(3, 10, 1, "debug", _.debug(msg), singlePageSuite1, case_s12)
  }

  val case_s13 = "singlePageSuite1: Log at info level"
  test(case_s13) {
    runTest(3, 10, 1, "info", _.info(msg), singlePageSuite1, case_s13)
  }

  val case_s14 = "singlePageSuite1: Log at warn level"
  test(case_s14) {
    runTest(3, 10, 1, "warn", _.warn(msg), singlePageSuite1, case_s14)
  }

  val case_s15 = "singlePageSuite1: Log at error level"
  test(case_s15) {
    runTest(3, 10, 1, "error", _.error(msg), singlePageSuite1, case_s15)
  }

  val singlePageSuite2 =
    "PagingSelfAwareStructuredLogger logs a message of 2 KB size and an exception in 1 log entry when page size is 10 KB"

  val case_s21 = "singlePageSuite2: Log at trace level"
  test(case_s21) {
    runTest(10, 10, 1, "trace", _.trace(excptn)(msg), singlePageSuite2, case_s21)
  }

  val case_s22 = "singlePageSuite2: Log at debug level"
  test(case_s22) {
    runTest(10, 10, 1, "debug", _.debug(excptn)(msg), singlePageSuite2, case_s22)
  }

  val case_s23 = "singlePageSuite2: Log at info level"
  test(case_s23) {
    runTest(10, 10, 1, "info", _.info(excptn)(msg), singlePageSuite2, case_s23)
  }

  val case_s24 = "singlePageSuite2: Log at warn level"
  test(case_s24) {
    runTest(10, 10, 1, "warn", _.warn(excptn)(msg), singlePageSuite2, case_s24)
  }

  val case_s25 = "singlePageSuite2: Log at error level"
  test(case_s25) {
    runTest(10, 10, 1, "error", _.error(excptn)(msg), singlePageSuite2, case_s25)
  }

  val singlePageSuite3 =
    "PagingSelfAwareStructuredLogger logs a message of 2 KB size with context in 1 log entry when page size is 3 KB"

  val case_s31 = "singlePageSuite3: Log at trace level"
  test(case_s31) {
    runTest(3, 10, 1, "trace", _.trace(ctx)(msg), singlePageSuite3, case_s31)
  }

  val case_s32 = "singlePageSuite3: Log at debug level"
  test(case_s32) {
    runTest(3, 10, 1, "debug", _.debug(ctx)(msg), singlePageSuite3, case_s32)
  }

  val case_s33 = "singlePageSuite3: Log at info level"
  test(case_s33) {
    runTest(3, 10, 1, "info", _.info(ctx)(msg), singlePageSuite3, case_s33)
  }

  val case_s34 = "singlePageSuite3: Log at warn level"
  test(case_s34) {
    runTest(3, 10, 1, "warn", _.warn(ctx)(msg), singlePageSuite3, case_s34)
  }

  val case_s35 = "singlePageSuite3: Log at error level"
  test(case_s35) {
    runTest(3, 10, 1, "error", _.error(ctx)(msg), singlePageSuite3, case_s35)
  }

  val singlePageSuite4 =
    "PagingSelfAwareStructuredLogger logs a message of 2 KB size with context and an exception in 1 log entry when page size is 10 KB"

  val case_s41 = "singlePageSuite4: Log at trace level"
  test(case_s41) {
    runTest(10, 10, 1, "trace", _.trace(ctx, excptn)(msg), singlePageSuite4, case_s41)
  }

  val case_s42 = "singlePageSuite4: Log at debug level"
  test(case_s42) {
    runTest(10, 10, 1, "debug", _.debug(ctx, excptn)(msg), singlePageSuite4, case_s42)
  }

  val case_s43 = "singlePageSuite4: Log at info level"
  test(case_s43) {
    runTest(10, 10, 1, "info", _.info(ctx, excptn)(msg), singlePageSuite4, case_s43)
  }

  val case_s44 = "singlePageSuite4: Log at warn level"
  test(case_s44) {
    runTest(10, 10, 1, "warn", _.warn(ctx, excptn)(msg), singlePageSuite4, case_s44)
  }

  val case_s45 = "singlePageSuite4: Log at error level"
  test(case_s45) {
    runTest(10, 10, 1, "error", _.error(ctx, excptn)(msg), singlePageSuite4, case_s45)
  }

  val multiplePageSuite1 =
    "PagingSelfAwareStructuredLogger logs a message of 2 KB size in 2 log entries when page size is 1 KB"

  val case_m11 = "multiplePageSuite1: Log at trace level"
  test(case_m11) {
    runTest(1, 10, 2, "trace", _.trace(msg), multiplePageSuite1, case_m11)
  }

  val case_m12 = "multiplePageSuite1: Log at debug level"
  test(case_m12) {
    runTest(1, 10, 2, "debug", _.debug(msg), multiplePageSuite1, case_m12)
  }

  val case_m13 = "multiplePageSuite1: Log at info level"
  test(case_m13) {
    runTest(1, 10, 2, "info", _.info(msg), multiplePageSuite1, case_m13)
  }

  val case_m14 = "multiplePageSuite1: Log at warn level"
  test(case_m14) {
    runTest(1, 10, 2, "warn", _.warn(msg), multiplePageSuite1, case_m14)
  }

  val case_m15 = "multiplePageSuite1: Log at error level"
  test(case_m15) {
    runTest(1, 10, 2, "error", _.error(msg), multiplePageSuite1, case_m15)
  }

  val multiplePageSuite2 =
    "PagingSelfAwareStructuredLogger logs a message of 2 KB size and an exception in 2 log entries when page size is 3 KB"

  val case_m21 = "multiplePageSuite2: Log at trace level"
  test(case_m21) {
    runTest(3, 10, 2, "trace", _.trace(excptn)(msg), multiplePageSuite2, case_m21)
  }

  val case_m22 = "multiplePageSuite2: Log at debug level"
  test(case_m22) {
    runTest(3, 10, 2, "debug", _.debug(excptn)(msg), multiplePageSuite2, case_m22)
  }

  val case_m23 = "multiplePageSuite2: Log at info level"
  test(case_m23) {
    runTest(3, 10, 2, "info", _.info(excptn)(msg), multiplePageSuite2, case_m23)
  }

  val case_m24 = "multiplePageSuite2: Log at warn level"
  test(case_m24) {
    runTest(3, 10, 2, "warn", _.warn(excptn)(msg), multiplePageSuite2, case_m24)
  }

  val case_m25 = "multiplePageSuite2: Log at error level"
  test(case_m25) {
    runTest(3, 10, 2, "error", _.error(excptn)(msg), multiplePageSuite2, case_m25)
  }

  val multiplePageSuite3 =
    "PagingSelfAwareStructuredLogger logs a message of 2 KB size with context in 2 log entries when page size is 1 KB"

  val case_m31 = "multiplePageSuite3: Log at trace level"
  test(case_m31) {
    runTest(1, 10, 2, "trace", _.trace(ctx)(msg), multiplePageSuite3, case_m31)
  }

  val case_m32 = "multiplePageSuite3: Log at debug level"
  test(case_m32) {
    runTest(1, 10, 2, "debug", _.debug(ctx)(msg), multiplePageSuite3, case_m32)
  }

  val case_m33 = "multiplePageSuite3: Log at info level"
  test(case_m33) {
    runTest(1, 10, 2, "info", _.info(ctx)(msg), multiplePageSuite3, case_m33)
  }

  val case_m34 = "multiplePageSuite3: Log at warn level"
  test(case_m34) {
    runTest(1, 10, 2, "warn", _.warn(ctx)(msg), multiplePageSuite3, case_m34)
  }

  val case_m35 = "multiplePageSuite3: Log at error level"
  test(case_m35) {
    runTest(1, 10, 2, "error", _.error(ctx)(msg), multiplePageSuite3, case_m35)
  }

  val multiplePageSuite4 =
    "PagingSelfAwareStructuredLogger logs a message of 2 KB size with context and an exception in 3 log entries when page size is 3 KB"

  val case_m41 = "multiplePageSuite4: Log at trace level"
  test(case_m41) {
    runTest(3, 10, 2, "trace", _.trace(ctx, excptn)(msg), multiplePageSuite4, case_m41)
  }

  val case_m42 = "multiplePageSuite4: Log at debug level"
  test(case_m42) {
    runTest(3, 10, 2, "debug", _.debug(ctx, excptn)(msg), multiplePageSuite4, case_m42)
  }

  val case_m43 = "multiplePageSuite4: Log at info level"
  test(case_m43) {
    runTest(3, 10, 2, "info", _.info(ctx, excptn)(msg), multiplePageSuite4, case_m43)
  }

  val case_m44 = "multiplePageSuite4: Log at warn level"
  test(case_m44) {
    runTest(3, 10, 2, "warn", _.warn(ctx, excptn)(msg), multiplePageSuite4, case_m44)
  }

  val case_m45 = "multiplePageSuite4: Log at error level"
  test(case_m45) {
    runTest(3, 10, 2, "error", _.error(ctx, excptn)(msg), multiplePageSuite4, case_m45)
  }

  val maxPageNumSuite =
    "PagingSelfAwareStructuredLogger with maxPageNeeded = 2 logs a message of 2 KB size with context and an exception in 2 log entries when page size is 3 KB"

  val case_mp1 = "maxPageNumSuite: Log at trace level"
  test(case_mp1) {
    runTest(3, 2, 2, "trace", _.trace(ctx, excptn)(msg), maxPageNumSuite, case_mp1)
  }

  val case_mp2 = "maxPageNumSuite: Log at debug level"
  test(case_mp2) {
    runTest(3, 2, 2, "debug", _.debug(ctx, excptn)(msg), maxPageNumSuite, case_mp2)
  }

  val case_mp3 = "maxPageNumSuite: Log at info level"
  test(case_mp3) {
    runTest(3, 2, 2, "info", _.info(ctx, excptn)(msg), maxPageNumSuite, case_mp3)
  }

  val case_mp4 = "maxPageNumSuite: Log at warn level"
  test(case_mp4) {
    runTest(3, 2, 2, "warn", _.warn(ctx, excptn)(msg), maxPageNumSuite, case_mp4)
  }

  val case_mp5 = "maxPageNumSuite: Log at error level"
  test(case_mp5) {
    runTest(3, 2, 2, "error", _.error(ctx, excptn)(msg), maxPageNumSuite, case_mp5)
  }

  val parameterChkSuite =
    "PagingSelfAwareStructuredLogger.withPaging() parameters pageSizeK and maxPageNeeded"

  val case_pc1 = "parameterChkSuite: Throw IllegalArgumentException for negative pageSizeK"
  test(case_pc1) {
    runTestExpectingThrow(-3, 2, parameterChkSuite, case_pc1)
  }

  val case_pc2 = "parameterChkSuite: Throw IllegalArgumentException for negative maxPageNeeded"
  test(case_pc2) {
    runTestExpectingThrow(3, -2, parameterChkSuite, case_pc2)
  }

  val case_pc3 = "parameterChkSuite: Throw IllegalArgumentException for pageSizeK=0"
  test(case_pc3) {
    runTestExpectingThrow(0, 2, parameterChkSuite, case_pc3)
  }

  val case_pc4 = "parameterChkSuite: Throw IllegalArgumentException for maxPageNeeded=0"
  test(case_pc4) {
    runTestExpectingThrow(3, 0, parameterChkSuite, case_pc4)
  }

  val case_pc5 = "parameterChkSuite: Throw IllegalArgumentException for pageSizeK=0, maxPageNeeded=0"
  test(case_pc5) {
    runTestExpectingThrow(0, 0, parameterChkSuite, case_pc5)
  }
}
