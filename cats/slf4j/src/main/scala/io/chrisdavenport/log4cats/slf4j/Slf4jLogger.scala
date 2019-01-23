/**
 * Copyright 2013-2017 Sarah Gerweck
 * see: https://github.com/Log4s/log4s
 *
 * Modifications copyright (C) 2018 Christopher Davenport
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
package io.chrisdavenport.log4cats.slf4j

import cats.effect.Sync
import cats._
import io.chrisdavenport.log4cats.SelfAwareStructuredLogger
import io.chrisdavenport.log4cats.slf4j.internal._
import org.slf4j.{Logger => JLogger}
import language.experimental.macros

object Slf4jLogger {


  def getLogger[F[_]: Sync]: SelfAwareStructuredLogger[F] =
    macro GetLoggerMacros.unsafeCreateImpl[F[_]]

  @deprecated("0.3.0", "Use getLogger instead")
  def unsafeCreate[F[_]: Sync]: SelfAwareStructuredLogger[F] =
    macro GetLoggerMacros.unsafeCreateImpl[F[_]]

  def getLoggerFromName[F[_]: Sync](name: String): SelfAwareStructuredLogger[F] =
    getLoggerFromSlf4j(org.slf4j.LoggerFactory.getLogger(name))

  @deprecated("0.3.0", "Use getLoggerFromName")
  def unsafeFromName[F[_]: Sync](name: String): SelfAwareStructuredLogger[F] =
    getLoggerFromName[F](name)

  def getLoggerFromClass[F[_]: Sync](clazz: Class[_]): SelfAwareStructuredLogger[F] =
    getLoggerFromSlf4j[F](org.slf4j.LoggerFactory.getLogger(clazz))

  @deprecated("0.3.0", "Use getLoggerFromClass")
  def unsafeFromClass[F[_]: Sync](clazz: Class[_]): SelfAwareStructuredLogger[F] =
    getLoggerFromClass[F](clazz)


  def getLoggerFromSlf4j[F[_]: Sync](logger: JLogger): SelfAwareStructuredLogger[F] =
    new Slf4jLoggerInternal.Slf4jLogger(logger)

  @deprecated("0.3.0", "Use getLoggerFromSlf4J instead")
  def unsafeFromSlf4j[F[_]: Sync](logger: JLogger): SelfAwareStructuredLogger[F] =
    getLoggerFromSlf4j[F](logger)


  def create[F[_]: Sync]: F[SelfAwareStructuredLogger[F]] =
    macro GetLoggerMacros.safeCreateImpl[F[_]]

  def fromName[F[_]: Sync](name: String): F[SelfAwareStructuredLogger[F]] =
    Sync[F].delay(getLoggerFromName(name))

  def fromClass[F[_]: Sync](clazz: Class[_]): F[SelfAwareStructuredLogger[F]] =
    Sync[F].delay(getLoggerFromClass(clazz))

  def fromSlf4j[F[_]: Sync](logger: JLogger): F[SelfAwareStructuredLogger[F]] =
    Sync[F].delay(getLoggerFromSlf4j[F](logger))



}

