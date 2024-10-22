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
package console

import cats.effect.kernel.Sync
import cats.syntax.all._
import org.typelevel.log4cats.extras.LogLevel
import org.typelevel.log4cats.extras.LogLevel._

class ConsoleLogger[F[_]: Sync](logLevel: Option[LogLevel] = Option(Trace))
    extends SelfAwareStructuredLogger[F] {
  private val ConsoleF: ConsoleF[F] = implicitly

  override def isEnabled(ll: LogLevel): F[Boolean] = logLevel.exists(_ <= ll).pure[F]

  override def log(ll: LogLevel, t: Throwable, msg: => String): F[Unit] =
    ll match {
      case LogLevel.Error => error(t)(msg)
      case LogLevel.Warn => warn(t)(msg)
      case LogLevel.Info => info(t)(msg)
      case LogLevel.Debug => debug(t)(msg)
      case LogLevel.Trace => trace(t)(msg)
    }

  override def log(ll: LogLevel, msg: => String): F[Unit] =
    ll match {
      case LogLevel.Error => error(msg)
      case LogLevel.Warn => warn(msg)
      case LogLevel.Info => info(msg)
      case LogLevel.Debug => debug(msg)
      case LogLevel.Trace => trace(msg)
    }

  override def trace(t: Throwable)(message: => String): F[Unit] = ConsoleF.debug(message, t)
  override def trace(message: => String): F[Unit] = ConsoleF.debug(message)

  override def debug(t: Throwable)(message: => String): F[Unit] = ConsoleF.debug(message, t)
  override def debug(message: => String): F[Unit] = ConsoleF.debug(message)

  override def info(t: Throwable)(message: => String): F[Unit] = ConsoleF.info(message, t)
  override def info(message: => String): F[Unit] = ConsoleF.info(message)

  override def warn(t: Throwable)(message: => String): F[Unit] = ConsoleF.warn(message, t)
  override def warn(message: => String): F[Unit] = ConsoleF.warn(message)

  override def error(t: Throwable)(message: => String): F[Unit] = ConsoleF.error(message, t)
  override def error(message: => String): F[Unit] = ConsoleF.error(message)

  /*
   * ConsoleLogger should probably not extend from StructuredLogger, because there's not
   * a good way to use the context map on this platform. However, LoggerFactory forces
   * its LoggerType to extend SelfAwareStructuredLogger, and since that's the factory
   * type that is well documented, that's what is demanded everywhere. Therefore, to be
   * useful, we implement the context variants below, but completely ignore the context
   * map parameters.
   */
  override def trace(ctx: Map[String, String])(msg: => String): F[Unit] = trace(msg)
  override def trace(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
    trace(t)(msg)
  override def debug(ctx: Map[String, String])(msg: => String): F[Unit] = debug(msg)
  override def debug(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
    debug(t)(msg)
  override def info(ctx: Map[String, String])(msg: => String): F[Unit] = info(msg)
  override def info(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] = info(t)(msg)
  override def warn(ctx: Map[String, String])(msg: => String): F[Unit] = warn(msg)
  override def warn(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] = warn(t)(msg)
  override def error(ctx: Map[String, String])(msg: => String): F[Unit] = error(msg)
  override def error(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
    error(t)(msg)
  override def log(ll: LogLevel, ctx: Map[String, String], t: Throwable, msg: => String): F[Unit] =
    log(ll, t, msg)
  override def log(ll: LogLevel, ctx: Map[String, String], msg: => String): F[Unit] = log(ll, msg)
}
