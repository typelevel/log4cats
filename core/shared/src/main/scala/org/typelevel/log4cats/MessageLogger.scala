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

trait MessageLogger[F[_]] {
  def error(message: => String): F[Unit] = log(LogLevel.Error, message)
  def warn(message: => String): F[Unit] = log(LogLevel.Warn, message)
  def info(message: => String): F[Unit] = log(LogLevel.Info, message)
  def debug(message: => String): F[Unit] = log(LogLevel.Debug, message)
  def trace(message: => String): F[Unit] = log(LogLevel.Trace, message)

  def log(ll: LogLevel, msg: => String): F[Unit]

  def mapK[G[_]](fk: F ~> G): MessageLogger[G] =
    MessageLogger.mapK(fk)(this)
}

object MessageLogger {
  def apply[F[_]](implicit ev: MessageLogger[F]): MessageLogger[F] = ev

  private def mapK[G[_], F[_]](f: G ~> F)(logger: MessageLogger[G]): MessageLogger[F] =
    new MessageLogger[F] {
      override def log(ll: LogLevel, msg: => String): F[Unit] = f(logger.log(ll, msg))
    }
}
