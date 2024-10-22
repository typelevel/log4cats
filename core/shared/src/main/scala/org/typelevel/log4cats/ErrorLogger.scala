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
trait ErrorLogger[F[_]] {
  def error(t: Throwable)(message: => String): F[Unit] = log(LogLevel.Error, t, message)
  def warn(t: Throwable)(message: => String): F[Unit] = log(LogLevel.Warn, t, message)
  def info(t: Throwable)(message: => String): F[Unit] = log(LogLevel.Info, t, message)
  def debug(t: Throwable)(message: => String): F[Unit] = log(LogLevel.Debug, t, message)
  def trace(t: Throwable)(message: => String): F[Unit] = log(LogLevel.Trace, t, message)

  def log(ll: LogLevel, t: Throwable, msg: => String): F[Unit]

  def mapK[G[_]](fk: F ~> G): ErrorLogger[G] =
    ErrorLogger.mapK(fk)(this)
}

object ErrorLogger {
  def apply[F[_]](implicit ev: ErrorLogger[F]): ErrorLogger[F] = ev

  private def mapK[G[_], F[_]](f: G ~> F)(logger: ErrorLogger[G]): ErrorLogger[F] =
    new ErrorLogger[F] {
      override def log(ll: LogLevel, t: Throwable, msg: => String): F[Unit] = f(
        logger.log(ll, t, msg)
      )
    }

}
