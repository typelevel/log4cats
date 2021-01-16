/*
 * Copyright 2020 Christopher Davenport
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

package org.typelevel.log4cats.extras

import org.typelevel.log4cats._
import cats._

/**
 * Allows for mapK or algebraic higher kinded
 * transformations
 */
object Translate {

  @deprecated("0.4.0", "Use mapK directly on the ErrorLogger")
  def errorLogger[G[_], F[_]](f: G ~> F)(logger: ErrorLogger[G]): ErrorLogger[F] =
    logger.mapK(f)

  @deprecated("0.4.0", "Use mapK directly on the Logger")
  def logger[G[_], F[_]](f: G ~> F)(logger: Logger[G]): Logger[F] =
    logger.mapK(f)

  @deprecated("0.4.0", "Use mapK directly on the MessageLogger")
  def messageLogger[G[_], F[_]](f: G ~> F)(logger: MessageLogger[G]): MessageLogger[F] =
    logger.mapK(f)

  @deprecated("0.4.0", "Use mapK directly on the SelfAwareLogger")
  def selfAwareLogger[G[_], F[_]](f: G ~> F)(logger: SelfAwareLogger[G]): SelfAwareLogger[F] =
    logger.mapK(f)

  @deprecated("0.4.0", "Use mapK directly on the SelfAwareStructuredLogger")
  def selfAwareStructuredLogger[G[_], F[_]](
      f: G ~> F
  )(logger: SelfAwareStructuredLogger[G]): SelfAwareStructuredLogger[F] =
    logger.mapK(f)

  @deprecated("0.4.0", "Use mapK directly on the StructuredLogger")
  def structuredLogger[G[_], F[_]](f: G ~> F)(logger: StructuredLogger[G]): StructuredLogger[F] =
    logger.mapK(f)

}
