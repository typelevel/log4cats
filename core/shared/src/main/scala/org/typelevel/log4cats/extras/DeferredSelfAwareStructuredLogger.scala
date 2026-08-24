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

import cats.data.Chain
import cats.effect.kernel.Resource.ExitCase
import cats.effect.kernel.{Concurrent, Ref, Resource}
import cats.syntax.all.*
import cats.{~>, Show}
import org.typelevel.log4cats.{KernelLogLevel, LoggerKernel, SelfAwareStructuredLogger}
import org.typelevel.log4cats.Log

/**
 * Similar to `DeferredStructuredLogger`, for `SelfAwareStructuredLogger`
 *
 * >>> WARNING: READ BEFORE USAGE! <<<
 * https://github.com/typelevel/log4cats/blob/main/core/shared/src/main/scala/org/typelevel/log4cats/extras/README.md
 * >>> WARNING: READ BEFORE USAGE! <<<
 */
trait DeferredSelfAwareStructuredLogger[F[_]]
    extends SelfAwareStructuredLogger[F]
    with DeferredLogging[F] {
  protected def kernel: LoggerKernel[F, String]

  override def mapK[G[_]](fk: F ~> G): DeferredSelfAwareStructuredLogger[G] =
    DeferredSelfAwareStructuredLogger.mapK(this, fk)

  override def addContext(ctx: Map[String, String]): DeferredSelfAwareStructuredLogger[F] =
    DeferredSelfAwareStructuredLogger.withContext(this)(ctx)

  override def addContext(pairs: (String, Show.Shown)*): DeferredSelfAwareStructuredLogger[F] =
    DeferredSelfAwareStructuredLogger.withContext(this)(
      pairs.map { case (k, v) => (k, v.toString) }.toMap
    )

  override def withModifiedString(f: String => String): DeferredSelfAwareStructuredLogger[F] =
    DeferredSelfAwareStructuredLogger.withModifiedString(this, f)
}
object DeferredSelfAwareStructuredLogger {
  def apply[F[_]: Concurrent](
      logger: SelfAwareStructuredLogger[F]
  ): Resource[F, DeferredSelfAwareStructuredLogger[F]] =
    makeCache[F].map(apply[F](logger, _))

  def apply[F[_]: Concurrent](
      logger: SelfAwareStructuredLogger[F],
      stash: Ref[F, Chain[(DeferredLogMessage, SelfAwareStructuredLogger[F])]]
  ): DeferredSelfAwareStructuredLogger[F] =
    new DeferredSelfAwareStructuredLogger[F] {
      private def save(lm: DeferredLogMessage): F[Unit] =
        stash.update(_.append(lm -> logger))

      protected def kernel: LoggerKernel[F, String] = new LoggerKernel[F, String] {
        def log(
            level: KernelLogLevel,
            logBuilder: Log.Builder[String] => Log.Builder[String]
        ): F[Unit] = {
          val log = logBuilder(Log.mutableBuilder[String]()).build()
          val logLevel = level match {
            case KernelLogLevel.Trace => LogLevel.Trace
            case KernelLogLevel.Debug => LogLevel.Debug
            case KernelLogLevel.Info => LogLevel.Info
            case KernelLogLevel.Warn => LogLevel.Warn
            case KernelLogLevel.Error => LogLevel.Error
            case KernelLogLevel.Fatal => LogLevel.Error
          }
          val deferredMsg = DeferredLogMessage(
            logLevel,
            log.context,
            log.throwable,
            log.message
          )
          save(deferredMsg)
        }
      }

      override def isTraceEnabled: F[Boolean] = logger.isTraceEnabled
      override def isDebugEnabled: F[Boolean] = logger.isDebugEnabled
      override def isInfoEnabled: F[Boolean] = logger.isInfoEnabled
      override def isWarnEnabled: F[Boolean] = logger.isWarnEnabled
      override def isErrorEnabled: F[Boolean] = logger.isErrorEnabled

      private def saveTrace(
          c: Map[String, String],
          t: Option[Throwable],
          m: () => String
      ): F[Unit] =
        isTraceEnabled.flatMap(save(DeferredLogMessage.trace(c, t, m)).whenA(_))
      private def saveDebug(
          c: Map[String, String],
          t: Option[Throwable],
          m: () => String
      ): F[Unit] =
        isDebugEnabled.flatMap(save(DeferredLogMessage.debug(c, t, m)).whenA(_))
      private def saveInfo(c: Map[String, String], t: Option[Throwable], m: () => String): F[Unit] =
        isInfoEnabled.flatMap(save(DeferredLogMessage.info(c, t, m)).whenA(_))
      private def saveWarn(c: Map[String, String], t: Option[Throwable], m: () => String): F[Unit] =
        isWarnEnabled.flatMap(save(DeferredLogMessage.warn(c, t, m)).whenA(_))
      private def saveError(
          c: Map[String, String],
          t: Option[Throwable],
          m: () => String
      ): F[Unit] =
        isErrorEnabled.flatMap(save(DeferredLogMessage.error(c, t, m)).whenA(_))

      override def trace(ctx: Map[String, String])(msg: => String): F[Unit] =
        saveTrace(ctx, none, () => msg)
      override def debug(ctx: Map[String, String])(msg: => String): F[Unit] =
        saveDebug(ctx, none, () => msg)
      override def info(ctx: Map[String, String])(msg: => String): F[Unit] =
        saveInfo(ctx, none, () => msg)
      override def warn(ctx: Map[String, String])(msg: => String): F[Unit] =
        saveWarn(ctx, none, () => msg)
      override def error(ctx: Map[String, String])(msg: => String): F[Unit] =
        saveError(ctx, none, () => msg)

      override def trace(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
        saveTrace(ctx, t.some, () => msg)
      override def debug(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
        saveDebug(ctx, t.some, () => msg)
      override def info(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
        saveInfo(ctx, t.some, () => msg)
      override def warn(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
        saveWarn(ctx, t.some, () => msg)
      override def error(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
        saveError(ctx, t.some, () => msg)

      override def trace(t: Throwable)(msg: => String): F[Unit] =
        saveTrace(Map.empty, t.some, () => msg)
      override def debug(t: Throwable)(msg: => String): F[Unit] =
        saveDebug(Map.empty, t.some, () => msg)
      override def info(t: Throwable)(msg: => String): F[Unit] =
        saveInfo(Map.empty, t.some, () => msg)
      override def warn(t: Throwable)(msg: => String): F[Unit] =
        saveWarn(Map.empty, t.some, () => msg)
      override def error(t: Throwable)(msg: => String): F[Unit] =
        saveError(Map.empty, t.some, () => msg)

      override def trace(msg: => String): F[Unit] = saveTrace(Map.empty, none, () => msg)
      override def debug(msg: => String): F[Unit] = saveDebug(Map.empty, none, () => msg)
      override def info(msg: => String): F[Unit] = saveInfo(Map.empty, none, () => msg)
      override def warn(msg: => String): F[Unit] = saveWarn(Map.empty, none, () => msg)
      override def error(msg: => String): F[Unit] = saveError(Map.empty, none, () => msg)

      override def inspect: F[Chain[DeferredLogMessage]] = stash.get.map(_._1F)

      override def log: F[Unit] = stash
        .getAndSet(Chain.empty)
        .flatMap(_.traverse_ { case (msg, logger) =>
          msg.logStructured(logger)
        })
    }

  private[extras] def makeCache[F[_]](implicit
      F: Concurrent[F]
  ): Resource[F, Ref[F, Chain[(DeferredLogMessage, SelfAwareStructuredLogger[F])]]] =
    Resource
      .makeCase(Ref.empty[F, Chain[(DeferredLogMessage, SelfAwareStructuredLogger[F])]]) {
        (ref, exitCase) =>
          exitCase match {
            case ExitCase.Succeeded => F.unit
            case _ =>
              ref.get.flatMap(_.traverse_ { case (msg, logger) => msg.logStructured(logger) })
          }
      }

  private def mapK[F[_], G[_]](
      logger: DeferredSelfAwareStructuredLogger[F],
      fk: F ~> G
  ): DeferredSelfAwareStructuredLogger[G] =
    new DeferredSelfAwareStructuredLogger[G] {
      protected def kernel: LoggerKernel[G, String] = logger.kernel.mapK(fk)

      override def inspect: G[Chain[DeferredLogMessage]] = fk(
        logger.inspect
      )
      override def log: G[Unit] = fk(logger.log)
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
      logger: DeferredSelfAwareStructuredLogger[F]
  )(baseCtx: Map[String, String]): DeferredSelfAwareStructuredLogger[F] =
    new DeferredSelfAwareStructuredLogger[F] {
      private def addCtx(ctx: Map[String, String]): Map[String, String] = baseCtx ++ ctx

      protected def kernel: LoggerKernel[F, String] = logger.kernel

      override def inspect: F[Chain[DeferredLogMessage]] = logger.inspect
      override def log: F[Unit] = logger.log
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
      logger: DeferredSelfAwareStructuredLogger[F],
      f: String => String
  ): DeferredSelfAwareStructuredLogger[F] =
    new DeferredSelfAwareStructuredLogger[F] {
      protected def kernel: LoggerKernel[F, String] = logger.kernel

      override def inspect: F[Chain[DeferredLogMessage]] = logger.inspect
      override def log: F[Unit] = logger.log
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
