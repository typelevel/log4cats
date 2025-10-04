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

/**
 * A SAM-based Logger that provides a user-friendly interface. This is the new design that will
 * eventually replace the current Logger trait.
 */
trait SamLogger[F[_], Ctx] {
  protected def kernel: LoggerKernel[F, Ctx]

  /** Access to the underlying kernel for advanced use cases */
  def underlying: LoggerKernel[F, Ctx] = kernel

  final def info(message: => String): F[Unit] = log_(KernelLogLevel.Info, message)

  final def warn(message: => String): F[Unit] = log_(KernelLogLevel.Warn, message)

  final def error(message: => String): F[Unit] = log_(KernelLogLevel.Error, message)

  final def trace(message: => String): F[Unit] = log_(KernelLogLevel.Trace, message)

  final def debug(message: => String): F[Unit] = log_(KernelLogLevel.Debug, message)

  private final def log_(
      level: KernelLogLevel,
      message: => String
  ): F[Unit] = {
    kernel.log(
      level,
      (record: Log.Builder[Ctx]) =>
        record
          .withLevel(level)
          .withMessage(message)
    )
  }

  def withModifiedString(f: String => String): SamLogger[F, Ctx] =
    SamLogger.withModifiedString[F, Ctx](this, f)
  def mapK[G[_]](fk: F ~> G): SamLogger[G, Ctx] = SamLogger.mapK(fk)(this)
}

object SamLogger {
  def apply[F[_], Ctx](implicit ev: SamLogger[F, Ctx]) = ev

  def wrap[F[_], Ctx](k: LoggerKernel[F, Ctx]): SamLogger[F, Ctx] = new SamLogger[F, Ctx] {
    protected def kernel: LoggerKernel[F, Ctx] = k
  }

  private def withModifiedString[F[_], Ctx](
      l: SamLogger[F, Ctx],
      f: String => String
  ): SamLogger[F, Ctx] =
    new SamLogger[F, Ctx] {
      protected def kernel: LoggerKernel[F, Ctx] = new LoggerKernel[F, Ctx] {
        def log(level: KernelLogLevel, record: Log.Builder[Ctx] => Log.Builder[Ctx]): F[Unit] =
          l.underlying.log(level, record(_).adaptMessage(f))
      }
    }

  private def mapK[G[_], F[_], Ctx](f: G ~> F)(logger: SamLogger[G, Ctx]): SamLogger[F, Ctx] =
    new SamLogger[F, Ctx] {
      protected def kernel: LoggerKernel[F, Ctx] = new LoggerKernel[F, Ctx] {
        def log(level: KernelLogLevel, record: Log.Builder[Ctx] => Log.Builder[Ctx]): F[Unit] =
          f(logger.underlying.log(level, record))
      }
    }
}
