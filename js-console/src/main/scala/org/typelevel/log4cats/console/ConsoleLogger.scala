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
import cats.syntax.all.*
import org.typelevel.log4cats.extras.LogLevel
import org.typelevel.log4cats.extras.LogLevel.*

class ConsoleLogger[F[_]: Sync](logLevel: Option[LogLevel] = Option(Trace))
    extends SelfAwareStructuredLogger[F] {
  private val ConsoleF: ConsoleF[F] = implicitly

  protected def kernel: LoggerKernel[F, String] = new LoggerKernel[F, String] {
    def log(level: KernelLogLevel, record: Log.Builder[String] => Log.Builder[String]): F[Unit] = {
      val logRecord = record(Log.mutableBuilder[String]())
      val log = logRecord.build()
      val message = log.message()
      val throwable = log.throwable

      level match {
        case KernelLogLevel.Trace => ConsoleF.debug(message, throwable.orNull)
        case KernelLogLevel.Debug => ConsoleF.debug(message, throwable.orNull)
        case KernelLogLevel.Info => ConsoleF.info(message, throwable.orNull)
        case KernelLogLevel.Warn => ConsoleF.warn(message, throwable.orNull)
        case KernelLogLevel.Error => ConsoleF.error(message, throwable.orNull)
        case KernelLogLevel.Fatal => ConsoleF.error(message, throwable.orNull)
        case _ =>
          ConsoleF.error(message, throwable.orNull) // Handle any other KernelLogLevel values
      }
    }
  }

  def isTraceEnabled: F[Boolean] = logLevel.exists(_ <= Trace).pure[F]
  def isDebugEnabled: F[Boolean] = logLevel.exists(_ <= Debug).pure[F]
  def isInfoEnabled: F[Boolean] = logLevel.exists(_ <= Info).pure[F]
  def isWarnEnabled: F[Boolean] = logLevel.exists(_ <= Warn).pure[F]
  def isErrorEnabled: F[Boolean] = logLevel.exists(_ <= Error).pure[F]

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
}
