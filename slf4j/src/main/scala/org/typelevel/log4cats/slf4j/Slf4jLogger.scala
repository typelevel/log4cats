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

import cats.effect.Sync
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.internal.Slf4jLoggerInternal
import org.slf4j.{Logger => JLogger}

object Slf4jLogger extends Slf4jLoggerCompat {

  def getLogger[F[_]](implicit f: Sync[F], name: LoggerName): SelfAwareStructuredLogger[F] =
    getLoggerFromName(name.value)

  def getLoggerFromName[F[_]: Sync](name: String): SelfAwareStructuredLogger[F] =
    getLoggerFromSlf4j(org.slf4j.LoggerFactory.getLogger(name))

  def getLoggerFromClass[F[_]: Sync](clazz: Class[_]): SelfAwareStructuredLogger[F] =
    getLoggerFromSlf4j[F](org.slf4j.LoggerFactory.getLogger(clazz))

  def getLoggerFromSlf4j[F[_]: Sync](logger: JLogger): SelfAwareStructuredLogger[F] =
    new Slf4jLoggerInternal.Slf4jLogger(logger)

  def create[F[_]: Sync](implicit name: LoggerName): F[SelfAwareStructuredLogger[F]] =
    Sync[F].delay(getLoggerFromName(name.value))

  def fromName[F[_]: Sync](name: String): F[SelfAwareStructuredLogger[F]] =
    Sync[F].delay(getLoggerFromName(name))

  def fromClass[F[_]: Sync](clazz: Class[_]): F[SelfAwareStructuredLogger[F]] =
    Sync[F].delay(getLoggerFromClass(clazz))

  def fromSlf4j[F[_]: Sync](logger: JLogger): F[SelfAwareStructuredLogger[F]] =
    Sync[F].delay(getLoggerFromSlf4j[F](logger))

  trait Factory[F[_]] extends LoggerFactoryGen[F, SelfAwareStructuredLogger[F]] {
    def getLoggerFromSlf4j(logger: JLogger): SelfAwareStructuredLogger[F]
    def fromSlf4j(logger: JLogger): F[SelfAwareStructuredLogger[F]]
  }

  object Factory extends LoggerFactoryCompanion {
    def apply[F[_]: Factory] = implicitly[Factory[F]]

    implicit def forSync[F[_]: Sync]: Factory[F] = new Factory[F] {
      override def getLoggerFromName(name: String): SelfAwareStructuredLogger[F] =
        Slf4jLogger.getLoggerFromName(name)
      override def getLoggerFromSlf4j(logger: JLogger): SelfAwareStructuredLogger[F] =
        Slf4jLogger.getLoggerFromSlf4j(logger)

      override def fromName(name: String): F[SelfAwareStructuredLogger[F]] =
        Slf4jLogger.fromName(name)

      override def fromSlf4j(logger: JLogger): F[SelfAwareStructuredLogger[F]] =
        Slf4jLogger.fromSlf4j(logger)
    }

    def getLoggerFromSlf4j[F[_]](logger: JLogger)(implicit
        lf: Factory[F]
    ): SelfAwareStructuredLogger[F] =
      lf.getLoggerFromSlf4j(logger)

    def fromSlf4j[F[_]](logger: JLogger)(implicit
        lf: Factory[F]
    ): F[SelfAwareStructuredLogger[F]] =
      lf.fromSlf4j(logger)
  }
}
