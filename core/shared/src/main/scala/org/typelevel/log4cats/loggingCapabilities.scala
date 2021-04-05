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

import com.lorandszakacs.enclosure.Enclosure

/**
 * Logging capability trait, or put crudeley "logger factory".
 *
 * The recommended way of creating loggers is through this capability trait.
 * You instantiate it once in your application (dependent on the specific
 * logging backend you use), and pass this around in your application.
 *
 * This has several advantages:
 *   - you no longer pass around _very powerful_ `F[_]: Sync` constraints that can do
 *     almost anything, when you just need logging
 *   - you are in control of how loggers are created, and you can even add in whatever
 *     custom functionality you need for your own applications here. e.g. create loggers
 *     that also send logs to some external providers by giving an implementation to this
 *     trait.
 */
trait LoggingGen[F[_], LoggerType <: Logger[F]]
    extends LoggingGenId[F, LoggerType]
    with LoggingGenF[F, LoggerType]

object LoggingGen {
  def apply[F[_], LoggerType <: Logger[F]](implicit
      l: LoggingGen[F, LoggerType]
  ): LoggingGen[F, LoggerType] = l
}

/**
 * Use when the creation of your loggers can be a pure operation.
 *
 * If you need to various side effects (init some state, make external calss, etc)
 * when instantiating a logger then use [[LoggingGenF]] instead.
 */
trait LoggingGenId[F[_], LoggerType <: Logger[F]] {
  def getLoggerFromName(name: String): LoggerType

  def getLogger(implicit enc: Enclosure): LoggerType =
    getLoggerFromName(enc.fullModuleName)

  def getLoggerFromClass(clazz: Class[_]): LoggerType =
    getLoggerFromName(clazz.getName()) //N.B. .getCanonicalName does not exist on scala JS.
}

object LoggingGenId {
  def apply[F[_], LoggerType <: Logger[F]](implicit
      l: LoggingGenId[F, LoggerType]
  ): LoggingGenId[F, LoggerType] = l
}

trait LoggingGenF[F[_], LoggerType <: Logger[F]] {
  def fromName(name: String): F[LoggerType]

  def create(implicit enc: Enclosure): F[LoggerType] =
    fromName(enc.fullModuleName)

  def fromClass(clazz: Class[_]): F[LoggerType] =
    fromName(clazz.getName) //N.B. .getCanonicalName does not exist on scala JS.
}

object LoggingGenF {
  def apply[F[_], LoggerType <: Logger[F]](implicit
      l: LoggingGenF[F, LoggerType]
  ): LoggingGenF[F, LoggerType] = l
}

object Logging {
  def apply[F[_]](implicit l: Logging[F]): Logging[F] = l
}

object LoggingId {
  def apply[F[_]](implicit l: LoggingId[F]): LoggingId[F] = l
}

object LoggingF {
  def apply[F[_]](implicit l: LoggingF[F]): LoggingF[F] = l
}
