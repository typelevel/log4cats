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

import cats.*
import org.typelevel.log4cats.extras.LogLevel

trait SelfAwareLogger[F[_]] extends Logger[F] {
  def isTraceEnabled: F[Boolean] = isEnabled(LogLevel.Trace)
  def isDebugEnabled: F[Boolean] = isEnabled(LogLevel.Debug)
  def isInfoEnabled: F[Boolean] = isEnabled(LogLevel.Info)
  def isWarnEnabled: F[Boolean] = isEnabled(LogLevel.Warn)
  def isErrorEnabled: F[Boolean] = isEnabled(LogLevel.Error)

  def isEnabled(ll: LogLevel): F[Boolean]

  override def mapK[G[_]](fk: F ~> G): SelfAwareLogger[G] = SelfAwareLogger.mapK(fk)(this)

  override def withModifiedString(f: String => String): SelfAwareLogger[F] =
    SelfAwareLogger.withModifiedString[F](this, f)
}
object SelfAwareLogger {
  def apply[F[_]](implicit ev: SelfAwareLogger[F]): SelfAwareLogger[F] = ev

  private def mapK[G[_], F[_]](f: G ~> F)(logger: SelfAwareLogger[G]): SelfAwareLogger[F] =
    new SelfAwareLogger[F] {
      override def isEnabled(ll: LogLevel): F[Boolean] = f(logger.isEnabled(ll))
      override def log(ll: LogLevel, t: Throwable, msg: => String): F[Unit] = f(
        logger.log(ll, t, msg)
      )
      override def log(ll: LogLevel, msg: => String): F[Unit] = f(logger.log(ll, msg))
    }

  private def withModifiedString[F[_]](
      l: SelfAwareLogger[F],
      f: String => String
  ): SelfAwareLogger[F] =
    new SelfAwareLogger[F] {
      override def isEnabled(ll: LogLevel): F[Boolean] = l.isEnabled(ll)

      override def log(ll: LogLevel, t: Throwable, msg: => String): F[Unit] = l.log(ll, t, f(msg))

      override def log(ll: LogLevel, msg: => String): F[Unit] = l.log(ll, f(msg))
    }
}
