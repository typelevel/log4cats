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

/**
 * SAM-based implementation of StructuredLogger that delegates to LoggerKernel. This provides the
 * same interface as StructuredLogger but uses the SAM architecture underneath for better
 * performance and middleware compatibility.
 */
trait SamStructuredLogger[F[_]] extends Logger[F] {
  protected def kernel: LoggerKernel[F]

  def trace(ctx: Map[String, String])(msg: => String): F[Unit] =
    kernel.logTrace(_.withMessage(msg).withContextMap(ctx))

  def trace(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
    kernel.logTrace(_.withMessage(msg).withContextMap(ctx).withThrowable(t))

  def debug(ctx: Map[String, String])(msg: => String): F[Unit] =
    kernel.logDebug(_.withMessage(msg).withContextMap(ctx))

  def debug(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
    kernel.logDebug(_.withMessage(msg).withContextMap(ctx).withThrowable(t))

  def info(ctx: Map[String, String])(msg: => String): F[Unit] =
    kernel.logInfo(_.withMessage(msg).withContextMap(ctx))

  def info(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
    kernel.logInfo(_.withMessage(msg).withContextMap(ctx).withThrowable(t))

  def warn(ctx: Map[String, String])(msg: => String): F[Unit] =
    kernel.logWarn(_.withMessage(msg).withContextMap(ctx))

  def warn(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
    kernel.logWarn(_.withMessage(msg).withContextMap(ctx).withThrowable(t))

  def error(ctx: Map[String, String])(msg: => String): F[Unit] =
    kernel.logError(_.withMessage(msg).withContextMap(ctx))

  def error(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
    kernel.logError(_.withMessage(msg).withContextMap(ctx).withThrowable(t))

  override def trace(message: => String): F[Unit] =
    kernel.logTrace(_.withMessage(message))

  override def trace(t: Throwable)(message: => String): F[Unit] =
    kernel.logTrace(_.withMessage(message).withThrowable(t))

  override def debug(message: => String): F[Unit] =
    kernel.logDebug(_.withMessage(message))

  override def debug(t: Throwable)(message: => String): F[Unit] =
    kernel.logDebug(_.withMessage(message).withThrowable(t))

  override def info(message: => String): F[Unit] =
    kernel.logInfo(_.withMessage(message))

  override def info(t: Throwable)(message: => String): F[Unit] =
    kernel.logInfo(_.withMessage(message).withThrowable(t))

  override def warn(message: => String): F[Unit] =
    kernel.logWarn(_.withMessage(message))

  override def warn(t: Throwable)(message: => String): F[Unit] =
    kernel.logWarn(_.withMessage(message).withThrowable(t))

  override def error(message: => String): F[Unit] =
    kernel.logError(_.withMessage(message))

  override def error(t: Throwable)(message: => String): F[Unit] =
    kernel.logError(_.withMessage(message).withThrowable(t))

  def addContext(ctx: Map[String, String]): SamStructuredLogger[F] =
    SamStructuredLogger.withContext(this)(ctx)

  def addContext(pairs: (String, Shown)*): SamStructuredLogger[F] =
    SamStructuredLogger.withContext(this)(
      pairs.map { case (k, v) => (k, v.toString) }.toMap
    )

  override def withModifiedString(f: String => String): SamStructuredLogger[F] =
    SamStructuredLogger.withModifiedString[F](this, f)

  override def mapK[G[_]](fk: F ~> G): SamStructuredLogger[G] =
    SamStructuredLogger.mapK(fk)(this)
}

object SamStructuredLogger {
  def apply[F[_]](implicit ev: SamStructuredLogger[F]): SamStructuredLogger[F] = ev

  def fromKernel[F[_]](kernelImpl: LoggerKernel[F]): SamStructuredLogger[F] =
    new SamStructuredLogger[F] {
      protected def kernel: LoggerKernel[F] = kernelImpl
    }

  def withContext[F[_]](sl: SamStructuredLogger[F])(
      ctx: Map[String, String]
  ): SamStructuredLogger[F] =
    new ModifiedContextSamStructuredLogger[F](sl)(ctx ++ _)

  def withModifiedContext[F[_]](
      sl: SamStructuredLogger[F]
  )(modifyCtx: Map[String, String] => Map[String, String]): SamStructuredLogger[F] =
    new ModifiedContextSamStructuredLogger[F](sl)(modifyCtx)

  private class ModifiedContextSamStructuredLogger[F[_]](sl: SamStructuredLogger[F])(
      modify: Map[String, String] => Map[String, String]
  ) extends SamStructuredLogger[F] {
    protected def kernel: LoggerKernel[F] = sl.kernel

    private lazy val defaultCtx: Map[String, String] = modify(Map.empty)

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

    override def trace(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
      sl.trace(modify(ctx), t)(msg)

    override def debug(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
      sl.debug(modify(ctx), t)(msg)

    override def info(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
      sl.info(modify(ctx), t)(msg)

    override def warn(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
      sl.warn(modify(ctx), t)(msg)

    override def error(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
      sl.error(modify(ctx), t)(msg)

    override def trace(message: => String): F[Unit] = sl.trace(defaultCtx)(message)
    override def trace(t: Throwable)(message: => String): F[Unit] = sl.trace(defaultCtx, t)(message)
    override def debug(message: => String): F[Unit] = sl.debug(defaultCtx)(message)
    override def debug(t: Throwable)(message: => String): F[Unit] = sl.debug(defaultCtx, t)(message)
    override def info(message: => String): F[Unit] = sl.info(defaultCtx)(message)
    override def info(t: Throwable)(message: => String): F[Unit] = sl.info(defaultCtx, t)(message)
    override def warn(message: => String): F[Unit] = sl.warn(defaultCtx)(message)
    override def warn(t: Throwable)(message: => String): F[Unit] = sl.warn(defaultCtx, t)(message)
    override def error(message: => String): F[Unit] = sl.error(defaultCtx)(message)
    override def error(t: Throwable)(message: => String): F[Unit] = sl.error(defaultCtx, t)(message)
  }

  private def withModifiedString[F[_]](
      l: SamStructuredLogger[F],
      f: String => String
  ): SamStructuredLogger[F] =
    new SamStructuredLogger[F] {
      protected def kernel: LoggerKernel[F] = l.kernel

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

      override def trace(message: => String): F[Unit] = l.trace(f(message))
      override def trace(t: Throwable)(message: => String): F[Unit] = l.trace(t)(f(message))
      override def debug(message: => String): F[Unit] = l.debug(f(message))
      override def debug(t: Throwable)(message: => String): F[Unit] = l.debug(t)(f(message))
      override def info(message: => String): F[Unit] = l.info(f(message))
      override def info(t: Throwable)(message: => String): F[Unit] = l.info(t)(f(message))
      override def warn(message: => String): F[Unit] = l.warn(f(message))
      override def warn(t: Throwable)(message: => String): F[Unit] = l.warn(t)(f(message))
      override def error(message: => String): F[Unit] = l.error(f(message))
      override def error(t: Throwable)(message: => String): F[Unit] = l.error(t)(f(message))
    }

  private def mapK[G[_], F[_]](f: G ~> F)(logger: SamStructuredLogger[G]): SamStructuredLogger[F] =
    new SamStructuredLogger[F] {
      protected def kernel: LoggerKernel[F] = new LoggerKernel[F] {
        def log(level: KernelLogLevel, record: Log.Builder => Log.Builder): F[Unit] =
          f(logger.kernel.log(level, record))
      }

      override def trace(ctx: Map[String, String])(msg: => String): F[Unit] =
        f(logger.trace(ctx)(msg))
      override def debug(ctx: Map[String, String])(msg: => String): F[Unit] =
        f(logger.debug(ctx)(msg))
      override def info(ctx: Map[String, String])(msg: => String): F[Unit] =
        f(logger.info(ctx)(msg))
      override def warn(ctx: Map[String, String])(msg: => String): F[Unit] =
        f(logger.warn(ctx)(msg))
      override def error(ctx: Map[String, String])(msg: => String): F[Unit] =
        f(logger.error(ctx)(msg))

      override def trace(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
        f(logger.trace(ctx, t)(msg))
      override def debug(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
        f(logger.debug(ctx, t)(msg))
      override def info(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
        f(logger.info(ctx, t)(msg))
      override def warn(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
        f(logger.warn(ctx, t)(msg))
      override def error(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
        f(logger.error(ctx, t)(msg))

      override def trace(message: => String): F[Unit] = f(logger.trace(message))
      override def trace(t: Throwable)(message: => String): F[Unit] = f(logger.trace(t)(message))
      override def debug(message: => String): F[Unit] = f(logger.debug(message))
      override def debug(t: Throwable)(message: => String): F[Unit] = f(logger.debug(t)(message))
      override def info(message: => String): F[Unit] = f(logger.info(message))
      override def info(t: Throwable)(message: => String): F[Unit] = f(logger.info(t)(message))
      override def warn(message: => String): F[Unit] = f(logger.warn(message))
      override def warn(t: Throwable)(message: => String): F[Unit] = f(logger.warn(t)(message))
      override def error(message: => String): F[Unit] = f(logger.error(message))
      override def error(t: Throwable)(message: => String): F[Unit] = f(logger.error(t)(message))
    }
}
