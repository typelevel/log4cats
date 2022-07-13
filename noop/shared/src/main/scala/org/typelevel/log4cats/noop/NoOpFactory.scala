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
package noop

import cats.Applicative

object NoOpFactory extends LoggerFactoryGenCompanion {
  def apply[F[_]: Applicative]: LoggerFactory[F] = impl[F]

  def impl[F[_]](implicit F: Applicative[F]): LoggerFactory[F] = new LoggerFactory[F] {
    override def getLoggerFromName(name: String): SelfAwareStructuredLogger[F] = {
      val _ = name
      NoOpLogger.impl[F]
    }

    override def fromName(name: String): F[SelfAwareStructuredLogger[F]] =
      F.pure(getLoggerFromName(name))
  }
}
