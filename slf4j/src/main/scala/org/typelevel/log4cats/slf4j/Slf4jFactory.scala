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
package slf4j

import org.slf4j.{Logger => JLogger}

trait Slf4jFactory[F[_]] extends LoggerFactory[F] {
  def getLoggerFromSlf4j(logger: JLogger): SelfAwareStructuredLogger[F]

  def fromSlf4j(logger: JLogger): F[SelfAwareStructuredLogger[F]]
}

object Slf4jFactory extends LoggerFactoryGenCompanion {
  def apply[F[_]: Slf4jFactory]: Slf4jFactory[F] = implicitly

  def getLoggerFromSlf4j[F[_]](logger: JLogger)(implicit
      lf: Slf4jFactory[F]
  ): SelfAwareStructuredLogger[F] = lf.getLoggerFromSlf4j(logger)

  def fromSlf4j[F[_]](logger: JLogger)(implicit
      lf: Slf4jFactory[F]
  ): F[SelfAwareStructuredLogger[F]] =
    lf.fromSlf4j(logger)
}
