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

trait StructuredLogger[F[_]] extends Logger[F] {
  // Structured logging methods
  def trace(ctx: Map[String, String])(msg: => String): F[Unit] =
    kernel.log(KernelLogLevel.Trace, _.withMessage(msg).withContextMap(ctx))

  def trace(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
    kernel.log(KernelLogLevel.Trace, _.withMessage(msg).withContextMap(ctx).withThrowable(t))

  def debug(ctx: Map[String, String])(msg: => String): F[Unit] =
    kernel.log(KernelLogLevel.Debug, _.withMessage(msg).withContextMap(ctx))

  def debug(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
    kernel.log(KernelLogLevel.Debug, _.withMessage(msg).withContextMap(ctx).withThrowable(t))

  def info(ctx: Map[String, String])(msg: => String): F[Unit] =
    kernel.log(KernelLogLevel.Info, _.withMessage(msg).withContextMap(ctx))

  def info(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
    kernel.log(KernelLogLevel.Info, _.withMessage(msg).withContextMap(ctx).withThrowable(t))

  def warn(ctx: Map[String, String])(msg: => String): F[Unit] =
    kernel.log(KernelLogLevel.Warn, _.withMessage(msg).withContextMap(ctx))

  def warn(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
    kernel.log(KernelLogLevel.Warn, _.withMessage(msg).withContextMap(ctx).withThrowable(t))

  def error(ctx: Map[String, String])(msg: => String): F[Unit] =
    kernel.log(KernelLogLevel.Error, _.withMessage(msg).withContextMap(ctx))

  def error(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
    kernel.log(KernelLogLevel.Error, _.withMessage(msg).withContextMap(ctx).withThrowable(t))

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
      l: StructuredLogger[F],
      f: String => String
  ): StructuredLogger[F] =
    new StructuredLogger[F] {
      protected def kernel: LoggerKernel[F, String] = l.underlying
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

  private def mapK[G[_], F[_]](f: G ~> F)(logger: StructuredLogger[G]): StructuredLogger[F] =
    new StructuredLogger[F] {
      protected def kernel: LoggerKernel[F, String] = logger.underlying.mapK(f)
    }
}
