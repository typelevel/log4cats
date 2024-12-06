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
import cats.Show.Shown
import org.typelevel.log4cats.extras.LogLevel

trait StructuredLogger[F[_]] extends Logger[F] {
  def trace(ctx: Map[String, String])(msg: => String): F[Unit] = log(LogLevel.Trace, ctx, msg)
  def trace(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
    log(LogLevel.Trace, ctx, t, msg)
  def debug(ctx: Map[String, String])(msg: => String): F[Unit] = log(LogLevel.Debug, ctx, msg)
  def debug(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
    log(LogLevel.Debug, ctx, t, msg)
  def info(ctx: Map[String, String])(msg: => String): F[Unit] = log(LogLevel.Info, ctx, msg)
  def info(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
    log(LogLevel.Info, ctx, t, msg)
  def warn(ctx: Map[String, String])(msg: => String): F[Unit] = log(LogLevel.Warn, ctx, msg)
  def warn(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
    log(LogLevel.Warn, ctx, t, msg)
  def error(ctx: Map[String, String])(msg: => String): F[Unit] = log(LogLevel.Error, ctx, msg)
  def error(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
    log(LogLevel.Error, ctx, t, msg)

  def log(ll: LogLevel, ctx: Map[String, String], msg: => String): F[Unit]
  def log(ll: LogLevel, ctx: Map[String, String], t: Throwable, msg: => String): F[Unit]

  override def mapK[G[_]](fk: F ~> G): StructuredLogger[G] =
    StructuredLogger.mapK(fk)(this)

  def addContext(ctx: Map[String, String]): StructuredLogger[F] =
    StructuredLogger.withContext(this)(ctx)

  def addContext(
      pairs: (String, Shown)*
  ): StructuredLogger[F] =
    StructuredLogger.withContext(this)(
      pairs.map { case (k, v) => (k, v.toString) }.toMap
    )

  override def withModifiedString(f: String => String): StructuredLogger[F] =
    StructuredLogger.withModifiedString[F](this, f)
}

object StructuredLogger {
  def apply[F[_]](implicit ev: StructuredLogger[F]): StructuredLogger[F] = ev

  def withContext[F[_]](sl: StructuredLogger[F])(ctx: Map[String, String]): StructuredLogger[F] =
    new ModifiedContextStructuredLogger[F](sl)(ctx ++ _)

  def withModifiedContext[F[_]](
      sl: StructuredLogger[F]
  )(modifyCtx: Map[String, String] => Map[String, String]): StructuredLogger[F] =
    new ModifiedContextStructuredLogger[F](sl)(modifyCtx)

  private class ModifiedContextStructuredLogger[F[_]](sl: StructuredLogger[F])(
      modify: Map[String, String] => Map[String, String]
  ) extends StructuredLogger[F] {
    private lazy val defaultCtx: Map[String, String] = modify(Map.empty)

    override def log(ll: LogLevel, ctx: Map[String, String], msg: => String): F[Unit] =
      sl.log(ll, modify(ctx), msg)

    override def log(
        ll: LogLevel,
        ctx: Map[String, String],
        t: Throwable,
        msg: => String
    ): F[Unit] =
      sl.log(ll, modify(ctx), t, msg)

    override def log(ll: LogLevel, t: Throwable, msg: => String): F[Unit] =
      sl.log(ll, defaultCtx, t, msg)

    override def log(ll: LogLevel, msg: => String): F[Unit] =
      sl.log(ll, defaultCtx, msg)
  }

  private def withModifiedString[F[_]](
      l: StructuredLogger[F],
      f: String => String
  ): StructuredLogger[F] =
    new StructuredLogger[F] {
      override def log(ll: LogLevel, ctx: Map[String, String], msg: => String): F[Unit] =
        l.log(ll, ctx, f(msg))

      override def log(
          ll: LogLevel,
          ctx: Map[String, String],
          t: Throwable,
          msg: => String
      ): F[Unit] =
        l.log(ll, ctx, t, f(msg))

      override def log(ll: LogLevel, t: Throwable, msg: => String): F[Unit] =
        l.log(ll, t, f(msg))

      override def log(ll: LogLevel, msg: => String): F[Unit] =
        l.log(ll, f(msg))
    }

  private def mapK[G[_], F[_]](f: G ~> F)(logger: StructuredLogger[G]): StructuredLogger[F] =
    new StructuredLogger[F] {
      override def log(ll: LogLevel, ctx: Map[String, String], msg: => String): F[Unit] =
        f(logger.log(ll, ctx, msg))

      override def log(
          ll: LogLevel,
          ctx: Map[String, String],
          t: Throwable,
          msg: => String
      ): F[Unit] =
        f(logger.log(ll, ctx, t, msg))

      override def log(ll: LogLevel, t: Throwable, msg: => String): F[Unit] =
        f(logger.log(ll, t, msg))

      override def log(ll: LogLevel, msg: => String): F[Unit] =
        f(logger.log(ll, msg))
    }
}
