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

package org.typelevel.log4cats.noop

import cats.effect.IO
import munit.CatsEffectSuite
import org.typelevel.log4cats._

class NoOpLoggerKernelTest extends CatsEffectSuite {
  private def boom()(implicit loc: munit.Location): String = fail(
    "This code should not have executed"
  )

  test("NoOpLoggerKernel should do nothing and not fail") {
    val kernel = NoOpLoggerKernel[IO, String]
    val logger = SamLogger.wrap(kernel)
    // All of these should be no-ops and not throw
    logger.info("This should not appear") *>
      logger.error(boom()) *>
      logger.warn(boom()) *>
      logger.debug(boom()) *>
      logger.trace(boom())
  }

  test("NoOpLoggerKernel should work with SamLogger") {
    val kernel = NoOpLoggerKernel[IO, String]
    val logger = SamLogger.wrap(kernel)
    logger.info("SamLogger test") *>
      logger.error("SamLogger test")
  }
}
