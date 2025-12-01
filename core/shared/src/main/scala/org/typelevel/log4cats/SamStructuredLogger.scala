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
 * A SAM-based StructuredLogger that extends Logger and provides structured logging capabilities.
 * This implementation uses the new SAM LoggerKernel design for better performance and middleware
 * compatibility.
 */
trait SamStructuredLogger[F[_]] extends Logger[F] {
  protected def kernel: LoggerKernel[F, String]

  final def trace(ctx: Map[String, String])(msg: => String): F[Unit] =
    kernel.logTrace(_.withMessage(msg).withContextMap(ctx))

  final def trace(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
    kernel.logTrace(_.withMessage(msg).withThrowable(t).withContextMap(ctx))

  final def debug(ctx: Map[String, String])(msg: => String): F[Unit] =
    kernel.logDebug(_.withMessage(msg).withContextMap(ctx))

  final def debug(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
    kernel.logDebug(_.withMessage(msg).withThrowable(t).withContextMap(ctx))

  final def info(ctx: Map[String, String])(msg: => String): F[Unit] =
    kernel.logInfo(_.withMessage(msg).withContextMap(ctx))

  final def info(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
    kernel.logInfo(_.withMessage(msg).withThrowable(t).withContextMap(ctx))

  final def warn(ctx: Map[String, String])(msg: => String): F[Unit] =
    kernel.logWarn(_.withMessage(msg).withContextMap(ctx))

  final def warn(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
    kernel.logWarn(_.withMessage(msg).withThrowable(t).withContextMap(ctx))

  final def error(ctx: Map[String, String])(msg: => String): F[Unit] =
    kernel.logError(_.withMessage(msg).withContextMap(ctx))

  final def error(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
    kernel.logError(_.withMessage(msg).withThrowable(t).withContextMap(ctx))

  final override def trace(message: => String): F[Unit] =
    kernel.logTrace(_.withMessage(message))

  final override def trace(t: Throwable)(message: => String): F[Unit] =
    kernel.logTrace(_.withMessage(message).withThrowable(t))

  final override def debug(message: => String): F[Unit] =
    kernel.logDebug(_.withMessage(message))

  final override def debug(t: Throwable)(message: => String): F[Unit] =
    kernel.logDebug(_.withMessage(message).withThrowable(t))

  final override def info(message: => String): F[Unit] =
    kernel.logInfo(_.withMessage(message))

  final override def info(t: Throwable)(message: => String): F[Unit] =
    kernel.logInfo(_.withMessage(message).withThrowable(t))

  final override def warn(message: => String): F[Unit] =
    kernel.logWarn(_.withMessage(message))

  final override def warn(t: Throwable)(message: => String): F[Unit] =
    kernel.logWarn(_.withMessage(message).withThrowable(t))

  final override def error(message: => String): F[Unit] =
    kernel.logError(_.withMessage(message))

  final override def error(t: Throwable)(message: => String): F[Unit] =
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

  def fromKernel[F[_]](kernelImpl: LoggerKernel[F, String]): SamStructuredLogger[F] =
    new SamStructuredLogger[F] {
      protected def kernel: LoggerKernel[F, String] = kernelImpl
    }

  def withContext[F[_]](sl: SamStructuredLogger[F])(
      ctx: Map[String, String]
  ): SamStructuredLogger[F] = withModifiedContext[F](sl)(ctx ++ _)

  def withModifiedContext[F[_]](
      sl: SamStructuredLogger[F]
  )(modifyCtx: Map[String, String] => Map[String, String]): SamStructuredLogger[F] =
    new SamStructuredLogger[F] {
      protected val kernel: LoggerKernel[F, String] =
        (level, record) => sl.kernel.log(level, record(_).adaptContext(modifyCtx))
    }

  private def withModifiedString[F[_]](
      l: SamStructuredLogger[F],
      f: String => String
  ): SamStructuredLogger[F] =
    new SamStructuredLogger[F] {
      protected val kernel: LoggerKernel[F, String] =
        (level, record) => l.kernel.log(level, record(_).adaptMessage(f))
    }

  private def mapK[G[_], F[_]](f: G ~> F)(logger: SamStructuredLogger[G]): SamStructuredLogger[F] =
    new SamStructuredLogger[F] {
      protected val kernel: LoggerKernel[F, String] = logger.kernel.mapK(f)
    }

}
