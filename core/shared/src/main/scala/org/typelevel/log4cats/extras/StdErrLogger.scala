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

package org.typelevel.log4cats.extras

import cats.effect.kernel.{Async, Ref}
import cats.effect.std.Console
import cats.syntax.all.*
import cats.{~>, Show}
import org.typelevel.log4cats.SelfAwareStructuredLogger

import java.time.Instant

/**
 * A simple logger that prints logs to standard error output.
 *
 * The intended use-case is for one-off scripts, examples, and other situations where a more
 * fully-featured and performance-optimized logger would be overkill
 *
 * Log output format is roughly:
 * {{{
 * %level %name - %message
 *   %ctx
 *   %throwable
 * }}}
 */
trait StdErrLogger[F[_]] extends SelfAwareStructuredLogger[F] {

  /**
   * Because there is no `log4j` backend, the log level can be set directly
   */
  def setLogLevel(level: LogLevel): F[Unit]

  override def mapK[G[_]](fk: F ~> G): StdErrLogger[G] =
    StdErrLogger.mapK(this, fk)

  override def addContext(ctx: Map[String, String]): StdErrLogger[F] =
    StdErrLogger.withContext(this)(ctx)

  override def addContext(pairs: (String, Show.Shown)*): StdErrLogger[F] =
    StdErrLogger.withContext(this)(
      pairs.map { case (k, v) => (k, v.toString) }.toMap
    )

  override def withModifiedString(f: String => String): StdErrLogger[F] =
    StdErrLogger.withModifiedString(this, f)
}
object StdErrLogger {
  def apply[F[_]: Console: Async](name: String, initialLogLevel: LogLevel): F[StdErrLogger[F]] =
    apply[F](name, initialLogLevel, LogFormatter.Default)

  def apply[F[_]](name: String, initialLogLevel: LogLevel, format: LogFormatter)(implicit
      console: Console[F],
      F: Async[F]
  ): F[StdErrLogger[F]] =
    Ref[F].of(initialLogLevel).map { logLevelRef =>
      new StdErrLogger[F] {
        override def setLogLevel(level: LogLevel): F[Unit] = logLevelRef.set(level)

        private def isLevelEnabled(level: LogLevel): F[Boolean] = logLevelRef.get.map(_ >= level)

        override def isTraceEnabled: F[Boolean] = isLevelEnabled(LogLevel.Trace)
        override def isDebugEnabled: F[Boolean] = isLevelEnabled(LogLevel.Debug)
        override def isInfoEnabled: F[Boolean] = isLevelEnabled(LogLevel.Info)
        override def isWarnEnabled: F[Boolean] = isLevelEnabled(LogLevel.Warn)
        override def isErrorEnabled: F[Boolean] = isLevelEnabled(LogLevel.Error)

        private def now: F[Instant] = F.realTime.map(d => Instant.EPOCH.plusNanos(d.toNanos))
        private def log(level: LogLevel, msg: => String): F[Unit] =
          isLevelEnabled(level).ifM(
            now.map(format.format(name, level, _, msg)).flatMap(console.errorln(_)),
            F.unit
          )

        private def log(level: LogLevel, msg: => String, throwable: Throwable): F[Unit] =
          isLevelEnabled(level).ifM(
            now.map(format.format(name, level, _, msg, throwable)).flatMap(console.errorln(_)),
            F.unit
          )

        private def log(level: LogLevel, msg: => String, ctx: Map[String, String]): F[Unit] =
          isLevelEnabled(level).ifM(
            now.map(format.format(name, level, _, msg, ctx)).flatMap(console.errorln(_)),
            F.unit
          )

        private def log(
            level: LogLevel,
            msg: => String,
            throwable: Throwable,
            ctx: Map[String, String]
        ): F[Unit] =
          isLevelEnabled(level).ifM(
            now.map(format.format(name, level, _, msg, ctx, throwable)).flatMap(console.errorln(_)),
            F.unit
          )

        override def trace(ctx: Map[String, String])(msg: => String): F[Unit] =
          log(LogLevel.Trace, msg, ctx)
        override def debug(ctx: Map[String, String])(msg: => String): F[Unit] =
          log(LogLevel.Debug, msg, ctx)
        override def info(ctx: Map[String, String])(msg: => String): F[Unit] =
          log(LogLevel.Info, msg, ctx)
        override def warn(ctx: Map[String, String])(msg: => String): F[Unit] =
          log(LogLevel.Warn, msg, ctx)
        override def error(ctx: Map[String, String])(msg: => String): F[Unit] =
          log(LogLevel.Error, msg, ctx)

        override def trace(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
          log(LogLevel.Trace, msg, t, ctx)
        override def debug(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
          log(LogLevel.Debug, msg, t, ctx)
        override def info(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
          log(LogLevel.Info, msg, t, ctx)
        override def warn(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
          log(LogLevel.Warn, msg, t, ctx)
        override def error(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
          log(LogLevel.Error, msg, t, ctx)

        override def error(t: Throwable)(message: => String): F[Unit] =
          log(LogLevel.Error, message, t)
        override def warn(t: Throwable)(message: => String): F[Unit] =
          log(LogLevel.Warn, message, t)
        override def info(t: Throwable)(message: => String): F[Unit] =
          log(LogLevel.Info, message, t)
        override def debug(t: Throwable)(message: => String): F[Unit] =
          log(LogLevel.Debug, message, t)
        override def trace(t: Throwable)(message: => String): F[Unit] =
          log(LogLevel.Trace, message, t)

        override def error(message: => String): F[Unit] = log(LogLevel.Error, message)
        override def warn(message: => String): F[Unit] = log(LogLevel.Warn, message)
        override def info(message: => String): F[Unit] = log(LogLevel.Info, message)
        override def debug(message: => String): F[Unit] = log(LogLevel.Debug, message)
        override def trace(message: => String): F[Unit] = log(LogLevel.Trace, message)
      }
    }

  private def mapK[F[_], G[_]](
      logger: StdErrLogger[F],
      fk: F ~> G
  ): StdErrLogger[G] =
    new StdErrLogger[G] {
      override def setLogLevel(level: LogLevel): G[Unit] = fk(logger.setLogLevel(level))

      override def isTraceEnabled: G[Boolean] = fk(logger.isTraceEnabled)
      override def isDebugEnabled: G[Boolean] = fk(logger.isDebugEnabled)
      override def isInfoEnabled: G[Boolean] = fk(logger.isInfoEnabled)
      override def isWarnEnabled: G[Boolean] = fk(logger.isWarnEnabled)
      override def isErrorEnabled: G[Boolean] = fk(logger.isErrorEnabled)

      override def trace(ctx: Map[String, String], t: Throwable)(msg: => String): G[Unit] = fk(
        logger.trace(ctx, t)(msg)
      )
      override def debug(ctx: Map[String, String], t: Throwable)(msg: => String): G[Unit] = fk(
        logger.debug(ctx, t)(msg)
      )
      override def info(ctx: Map[String, String], t: Throwable)(msg: => String): G[Unit] = fk(
        logger.info(ctx, t)(msg)
      )
      override def warn(ctx: Map[String, String], t: Throwable)(msg: => String): G[Unit] = fk(
        logger.warn(ctx, t)(msg)
      )
      override def error(ctx: Map[String, String], t: Throwable)(msg: => String): G[Unit] = fk(
        logger.error(ctx, t)(msg)
      )

      override def trace(ctx: Map[String, String])(msg: => String): G[Unit] = fk(
        logger.trace(ctx)(msg)
      )
      override def debug(ctx: Map[String, String])(msg: => String): G[Unit] = fk(
        logger.debug(ctx)(msg)
      )
      override def info(ctx: Map[String, String])(msg: => String): G[Unit] = fk(
        logger.info(ctx)(msg)
      )
      override def warn(ctx: Map[String, String])(msg: => String): G[Unit] = fk(
        logger.warn(ctx)(msg)
      )
      override def error(ctx: Map[String, String])(msg: => String): G[Unit] = fk(
        logger.error(ctx)(msg)
      )

      override def trace(t: Throwable)(message: => String): G[Unit] = fk(logger.trace(t)(message))
      override def debug(t: Throwable)(message: => String): G[Unit] = fk(logger.debug(t)(message))
      override def info(t: Throwable)(message: => String): G[Unit] = fk(logger.info(t)(message))
      override def warn(t: Throwable)(message: => String): G[Unit] = fk(logger.warn(t)(message))
      override def error(t: Throwable)(message: => String): G[Unit] = fk(logger.error(t)(message))

      override def trace(message: => String): G[Unit] = fk(logger.trace(message))
      override def debug(message: => String): G[Unit] = fk(logger.debug(message))
      override def info(message: => String): G[Unit] = fk(logger.info(message))
      override def warn(message: => String): G[Unit] = fk(logger.warn(message))
      override def error(message: => String): G[Unit] = fk(logger.error(message))
    }

  def withContext[F[_]](
      logger: StdErrLogger[F]
  )(baseCtx: Map[String, String]): StdErrLogger[F] =
    new StdErrLogger[F] {
      private def addCtx(ctx: Map[String, String]): Map[String, String] = baseCtx ++ ctx

      override def setLogLevel(level: LogLevel): F[Unit] = logger.setLogLevel(level)

      override def isTraceEnabled: F[Boolean] = logger.isTraceEnabled
      override def isDebugEnabled: F[Boolean] = logger.isDebugEnabled
      override def isInfoEnabled: F[Boolean] = logger.isInfoEnabled
      override def isWarnEnabled: F[Boolean] = logger.isWarnEnabled
      override def isErrorEnabled: F[Boolean] = logger.isErrorEnabled

      override def trace(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
        logger.trace(addCtx(ctx), t)(msg)
      override def debug(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
        logger.debug(addCtx(ctx), t)(msg)
      override def info(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
        logger.info(addCtx(ctx), t)(msg)
      override def warn(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
        logger.warn(addCtx(ctx), t)(msg)
      override def error(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
        logger.error(addCtx(ctx), t)(msg)

      override def trace(ctx: Map[String, String])(msg: => String): F[Unit] =
        logger.trace(addCtx(ctx))(msg)
      override def debug(ctx: Map[String, String])(msg: => String): F[Unit] =
        logger.debug(addCtx(ctx))(msg)
      override def info(ctx: Map[String, String])(msg: => String): F[Unit] =
        logger.info(addCtx(ctx))(msg)
      override def warn(ctx: Map[String, String])(msg: => String): F[Unit] =
        logger.warn(addCtx(ctx))(msg)
      override def error(ctx: Map[String, String])(msg: => String): F[Unit] =
        logger.error(addCtx(ctx))(msg)

      override def trace(t: Throwable)(message: => String): F[Unit] =
        logger.trace(baseCtx, t)(message)
      override def debug(t: Throwable)(message: => String): F[Unit] =
        logger.debug(baseCtx, t)(message)
      override def info(t: Throwable)(message: => String): F[Unit] =
        logger.info(baseCtx, t)(message)
      override def warn(t: Throwable)(message: => String): F[Unit] =
        logger.warn(baseCtx, t)(message)
      override def error(t: Throwable)(message: => String): F[Unit] =
        logger.error(baseCtx, t)(message)

      override def trace(message: => String): F[Unit] = logger.trace(baseCtx)(message)
      override def debug(message: => String): F[Unit] = logger.debug(baseCtx)(message)
      override def info(message: => String): F[Unit] = logger.info(baseCtx)(message)
      override def warn(message: => String): F[Unit] = logger.warn(baseCtx)(message)
      override def error(message: => String): F[Unit] = logger.error(baseCtx)(message)
    }

  def withModifiedString[F[_]](
      logger: StdErrLogger[F],
      f: String => String
  ): StdErrLogger[F] =
    new StdErrLogger[F] {
      override def setLogLevel(level: LogLevel): F[Unit] = logger.setLogLevel(level)

      override def isTraceEnabled: F[Boolean] = logger.isTraceEnabled
      override def isDebugEnabled: F[Boolean] = logger.isDebugEnabled
      override def isInfoEnabled: F[Boolean] = logger.isInfoEnabled
      override def isWarnEnabled: F[Boolean] = logger.isWarnEnabled
      override def isErrorEnabled: F[Boolean] = logger.isErrorEnabled

      override def trace(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
        logger.trace(ctx, t)(f(msg))
      override def debug(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
        logger.debug(ctx, t)(f(msg))
      override def info(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
        logger.info(ctx, t)(f(msg))
      override def warn(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
        logger.warn(ctx, t)(f(msg))
      override def error(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
        logger.error(ctx, t)(f(msg))

      override def trace(ctx: Map[String, String])(msg: => String): F[Unit] =
        logger.trace(ctx)(f(msg))
      override def debug(ctx: Map[String, String])(msg: => String): F[Unit] =
        logger.debug(ctx)(f(msg))
      override def info(ctx: Map[String, String])(msg: => String): F[Unit] =
        logger.info(ctx)(f(msg))
      override def warn(ctx: Map[String, String])(msg: => String): F[Unit] =
        logger.warn(ctx)(f(msg))
      override def error(ctx: Map[String, String])(msg: => String): F[Unit] =
        logger.error(ctx)(f(msg))

      override def trace(t: Throwable)(message: => String): F[Unit] = logger.trace(t)(f(message))
      override def debug(t: Throwable)(message: => String): F[Unit] = logger.debug(t)(f(message))
      override def info(t: Throwable)(message: => String): F[Unit] = logger.info(t)(f(message))
      override def warn(t: Throwable)(message: => String): F[Unit] = logger.warn(t)(f(message))
      override def error(t: Throwable)(message: => String): F[Unit] = logger.error(t)(f(message))

      override def trace(message: => String): F[Unit] = logger.trace(f(message))
      override def debug(message: => String): F[Unit] = logger.debug(f(message))
      override def info(message: => String): F[Unit] = logger.info(f(message))
      override def warn(message: => String): F[Unit] = logger.warn(f(message))
      override def error(message: => String): F[Unit] = logger.error(f(message))
    }
}
