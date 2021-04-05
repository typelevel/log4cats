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

package org.typelevel.log4cats.noop

import com.lorandszakacs.enclosure.Enclosure
import cats.syntax.all._
import cats.Applicative
import org.typelevel.log4cats._

trait NoopLogging[F[_]] extends Logging[F] {
  implicit protected def applicative: Applicative[F]

  override def getLogger(implicit enc: Enclosure): SelfAwareStructuredLogger[F] = NoOpLogger.impl[F]

  override def getLoggerFromName(name: String): SelfAwareStructuredLogger[F] = NoOpLogger.impl[F]

  override def getLoggerFromClass(clazz: Class[_]): SelfAwareStructuredLogger[F] =
    NoOpLogger.impl[F]

  override def create(implicit enc: Enclosure): F[SelfAwareStructuredLogger[F]] =
    NoOpLogger.impl[F].pure[F]

  override def fromName(name: String): F[SelfAwareStructuredLogger[F]] = NoOpLogger.impl[F].pure[F]

  override def fromClass(clazz: Class[_]): F[SelfAwareStructuredLogger[F]] =
    NoOpLogger.impl[F].pure[F]

}

object NoopLogging {
  def apply[F[_]](implicit logging: NoopLogging[F]): NoopLogging[F] = logging
  def create[F[_]](implicit F: Applicative[F]) = new NoopLogging[F] {
    override implicit protected val applicative: Applicative[F] = F
  }
}
