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

package org.typelevel.log4cats.slf4j

import org.slf4j.{Logger => JLogger}
import cats.effect.Sync
import org.typelevel.log4cats._

trait Slf4jLogging[F[_]] extends Logging[F] {

  implicit protected def sync: Sync[F]

  override def getLoggerFromName(name: String): SelfAwareStructuredLogger[F] =
    Slf4jLogger.getLoggerFromName[F](name)

  def getLoggerFromSlf4j(logger: JLogger): SelfAwareStructuredLogger[F] =
    Slf4jLogger.getLoggerFromSlf4j[F](logger)

  override def fromName(name: String): F[SelfAwareStructuredLogger[F]] =
    Slf4jLogger.fromName[F](name)

  def fromSlf4j(logger: JLogger): F[SelfAwareStructuredLogger[F]] =
    Slf4jLogger.fromSlf4j[F](logger)
}

object Slf4jLogging {
  def apply[F[_]](implicit logging: Slf4jLogging[F]): Slf4jLogging[F] = logging

  def createForSync[F[_]](implicit F: Sync[F]): Slf4jLogging[F] = new Slf4jLogging[F] {
    override protected val sync: Sync[F] = F
  }
}
