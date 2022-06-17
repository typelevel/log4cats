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

trait LoggerFactoryGen[F[_]] {
  type LoggerType <: Logger[F]
  def getLogger(implicit name: LoggerName): LoggerType = getLoggerFromName(name.value)
  def getLoggerFromClass(clazz: Class[_]): LoggerType = getLoggerFromName(clazz.getName)
  def create(implicit name: LoggerName): F[LoggerType] = fromName(name.value)
  def fromClass(clazz: Class[_]): F[LoggerType] = fromName(clazz.getName)
  def getLoggerFromName(name: String): LoggerType
  def fromName(name: String): F[LoggerType]
}

private[log4cats] trait LoggerFactoryGenCompanion {
  def getLogger[F[_]](implicit lf: LoggerFactoryGen[F], name: LoggerName): lf.LoggerType =
    lf.getLogger
  def getLoggerFromName[F[_]](name: String)(implicit lf: LoggerFactoryGen[F]): lf.LoggerType =
    lf.getLoggerFromName(name)
  def getLoggerFromClass[F[_]](clazz: Class[_])(implicit lf: LoggerFactoryGen[F]): lf.LoggerType =
    lf.getLoggerFromClass(clazz)
  def create[F[_]](implicit lf: LoggerFactoryGen[F], name: LoggerName): F[lf.LoggerType] =
    lf.create
  def fromName[F[_]](name: String)(implicit lf: LoggerFactoryGen[F]): F[lf.LoggerType] =
    lf.fromName(name)
  def fromClass[F[_]](clazz: Class[_])(implicit lf: LoggerFactoryGen[F]): F[lf.LoggerType] =
    lf.fromClass(clazz)
}
