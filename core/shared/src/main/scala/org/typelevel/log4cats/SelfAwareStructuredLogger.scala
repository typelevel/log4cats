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
    protected def kernel: LoggerKernel[F, String] = sl.underlying
    private lazy val defaultCtx: Map[String, String] = modify(Map.empty)

    override def error(message: => String): F[Unit] = sl.error(defaultCtx)(message)
    override def warn(message: => String): F[Unit] = sl.warn(defaultCtx)(message)
    override def info(message: => String): F[Unit] = sl.info(defaultCtx)(message)
    override def debug(message: => String): F[Unit] = sl.debug(defaultCtx)(message)
    override def trace(message: => String): F[Unit] = sl.trace(defaultCtx)(message)

    override def trace(ctx: Map[String, String])(msg: => String): F[Unit] =
      sl.trace(modify(ctx))(msg)
    override def debug(ctx: Map[String, String])(msg: => String): F[Unit] =
      sl.debug(modify(ctx))(msg)
    override def info(ctx: Map[String, String])(msg: => String): F[Unit] =
      sl.info(modify(ctx))(msg)
    override def warn(ctx: Map[String, String])(msg: => String): F[Unit] =
      sl.warn(modify(ctx))(msg)
    override def error(ctx: Map[String, String])(msg: => String): F[Unit] =
      sl.error(modify(ctx))(msg)

    override def isTraceEnabled: F[Boolean] = sl.isTraceEnabled
    override def isDebugEnabled: F[Boolean] = sl.isDebugEnabled
    override def isInfoEnabled: F[Boolean] = sl.isInfoEnabled
    override def isWarnEnabled: F[Boolean] = sl.isWarnEnabled
    override def isErrorEnabled: F[Boolean] = sl.isErrorEnabled

    override def error(t: Throwable)(message: => String): F[Unit] =
      sl.error(defaultCtx, t)(message)
    override def warn(t: Throwable)(message: => String): F[Unit] =
      sl.warn(defaultCtx, t)(message)
    override def info(t: Throwable)(message: => String): F[Unit] =
      sl.info(defaultCtx, t)(message)
    override def debug(t: Throwable)(message: => String): F[Unit] =
      sl.debug(defaultCtx, t)(message)
    override def trace(t: Throwable)(message: => String): F[Unit] =
      sl.trace(defaultCtx, t)(message)

    override def error(ctx: Map[String, String], t: Throwable)(message: => String): F[Unit] =
      sl.error(modify(ctx), t)(message)
    override def warn(ctx: Map[String, String], t: Throwable)(message: => String): F[Unit] =
      sl.warn(modify(ctx), t)(message)
    override def info(ctx: Map[String, String], t: Throwable)(message: => String): F[Unit] =
      sl.info(modify(ctx), t)(message)
    override def debug(ctx: Map[String, String], t: Throwable)(message: => String): F[Unit] =
      sl.debug(modify(ctx), t)(message)
    override def trace(ctx: Map[String, String], t: Throwable)(message: => String): F[Unit] =
      sl.trace(modify(ctx), t)(message)
  }

  private def withModifiedString[F[_]](
      l: SelfAwareStructuredLogger[F],
      f: String => String
  ): SelfAwareStructuredLogger[F] =
    new SelfAwareStructuredLogger[F] {
      protected def kernel: LoggerKernel[F, String] = l.underlying

      def isTraceEnabled: F[Boolean] = l.isTraceEnabled
      def isDebugEnabled: F[Boolean] = l.isDebugEnabled
      def isInfoEnabled: F[Boolean] = l.isInfoEnabled
      def isWarnEnabled: F[Boolean] = l.isWarnEnabled
      def isErrorEnabled: F[Boolean] = l.isErrorEnabled

      override def trace(ctx: Map[String, String])(msg: => String): F[Unit] = l.trace(ctx)(f(msg))
      override def trace(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
        l.trace(ctx, t)(f(msg))
      override def debug(ctx: Map[String, String])(msg: => String): F[Unit] = l.debug(ctx)(f(msg))
      override def debug(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
        l.debug(ctx, t)(f(msg))
      override def info(ctx: Map[String, String])(msg: => String): F[Unit] = l.info(ctx)(f(msg))
      override def info(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
        l.info(ctx, t)(f(msg))
      override def warn(ctx: Map[String, String])(msg: => String): F[Unit] = l.warn(ctx)(f(msg))
      override def warn(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
        l.warn(ctx, t)(f(msg))
      override def error(ctx: Map[String, String])(msg: => String): F[Unit] = l.error(ctx)(f(msg))
      override def error(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
        l.error(ctx, t)(f(msg))
      override def error(message: => String): F[Unit] = l.error(f(message))
      override def error(t: Throwable)(message: => String): F[Unit] = l.error(t)(f(message))
      override def warn(message: => String): F[Unit] = l.warn(f(message))
      override def warn(t: Throwable)(message: => String): F[Unit] = l.warn(t)(f(message))
      override def info(message: => String): F[Unit] = l.info(f(message))
      override def info(t: Throwable)(message: => String): F[Unit] = l.info(t)(f(message))
      override def debug(message: => String): F[Unit] = l.debug(f(message))
      override def debug(t: Throwable)(message: => String): F[Unit] = l.debug(t)(f(message))
      override def trace(message: => String): F[Unit] = l.trace(f(message))
      override def trace(t: Throwable)(message: => String): F[Unit] = l.trace(t)(f(message))
    }

  private def mapK[G[_], F[_]](
      f: G ~> F
  )(logger: SelfAwareStructuredLogger[G]): SelfAwareStructuredLogger[F] =
    new SelfAwareStructuredLogger[F] {
      protected def kernel: LoggerKernel[F, String] = logger.underlying.mapK(f)

      def isTraceEnabled: F[Boolean] = f(logger.isTraceEnabled)
      def isDebugEnabled: F[Boolean] = f(logger.isDebugEnabled)
      def isInfoEnabled: F[Boolean] = f(logger.isInfoEnabled)
      def isWarnEnabled: F[Boolean] = f(logger.isWarnEnabled)
      def isErrorEnabled: F[Boolean] = f(logger.isErrorEnabled)
    }
}
