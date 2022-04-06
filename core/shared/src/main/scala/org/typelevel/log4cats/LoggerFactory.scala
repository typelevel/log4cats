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

import scala.annotation.implicitNotFound

@implicitNotFound("""|
                     |Implicit not found for LoggerFactory[${F}].
                     |
                     |Information can be found here: https://log4cats.github.io/logging-capability.html
                     |""")
trait LoggerFactory[F[_]] extends LoggerFactoryGen[F, SelfAwareStructuredLogger[F]]
object LoggerFactory extends LoggerFactoryCompanion

private[log4cats] trait LoggerFactoryCompanion {
  def getLogger[F[_]](implicit
      lf: LoggerFactory[F],
      name: LoggerName
  ): SelfAwareStructuredLogger[F] =
    lf.getLogger
  def getLoggerFromName[F[_]](name: String)(implicit
      lf: LoggerFactory[F]
  ): SelfAwareStructuredLogger[F] =
    lf.getLoggerFromName(name)

  def getLoggerFromClass[F[_]](clazz: Class[_])(implicit
      lf: LoggerFactory[F]
  ): SelfAwareStructuredLogger[F] =
    lf.getLoggerFromClass(clazz)

  def create[F[_]](implicit
      lf: LoggerFactory[F],
      name: LoggerName
  ): F[SelfAwareStructuredLogger[F]] =
    lf.create
  def fromName[F[_]](name: String)(implicit lf: LoggerFactory[F]): F[SelfAwareStructuredLogger[F]] =
    lf.fromName(name)
  def fromClass[F[_]](clazz: Class[_])(implicit
      lf: LoggerFactory[F]
  ): F[SelfAwareStructuredLogger[F]] =
    lf.fromClass(clazz)
}
