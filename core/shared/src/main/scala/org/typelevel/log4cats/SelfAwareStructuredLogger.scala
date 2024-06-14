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

trait SelfAwareStructuredLogger[F[_]] extends SelfAwareLogger[F] with StructuredLogger[F] {
  override def mapK[G[_]](fk: F ~> G): SelfAwareStructuredLogger[G] =
    SelfAwareStructuredLogger.mapK(fk)(this)

  override def addContext(ctx: Map[String, String]): SelfAwareStructuredLogger[F] =
    SelfAwareStructuredLogger.withContext(this)(ctx)

  override def addContext(
      pairs: (String, Shown)*
  ): SelfAwareStructuredLogger[F] =
    SelfAwareStructuredLogger.withContext(this)(
      pairs.map { case (k, v) => (k, v.toString) }.toMap
    )

  override def withModifiedString(f: String => String): SelfAwareStructuredLogger[F] =
    SelfAwareStructuredLogger.withModifiedString[F](this, f)
}

object SelfAwareStructuredLogger {
  def apply[F[_]](implicit ev: SelfAwareStructuredLogger[F]): SelfAwareStructuredLogger[F] = ev

  def withContext[F[_]](
      sl: SelfAwareStructuredLogger[F]
  )(ctx: Map[String, String]): SelfAwareStructuredLogger[F] =
    new ModifiedContextSelfAwareStructuredLogger[F](sl)(ctx ++ _)

  private class ModifiedContextSelfAwareStructuredLogger[F[_]](sl: SelfAwareStructuredLogger[F])(
      modify: Map[String, String] => Map[String, String]
  ) extends SelfAwareStructuredLogger[F] {
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

    override def isEnabled(ll: LogLevel): F[Boolean] = sl.isEnabled(ll)
  }

  private def withModifiedString[F[_]](
      l: SelfAwareStructuredLogger[F],
      f: String => String
  ): SelfAwareStructuredLogger[F] =
    new SelfAwareStructuredLogger[F] {
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

      override def isEnabled(ll: LogLevel): F[Boolean] = l.isEnabled(ll)
    }

  private def mapK[G[_], F[_]](
      f: G ~> F
  )(logger: SelfAwareStructuredLogger[G]): SelfAwareStructuredLogger[F] =
    new SelfAwareStructuredLogger[F] {
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

      override def isEnabled(ll: LogLevel): F[Boolean] = f(logger.isEnabled(ll))
    }
}
