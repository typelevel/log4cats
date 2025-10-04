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

import cats.Show.Shown
import cats.data.Chain
import cats.effect.kernel.Resource.ExitCase
import cats.effect.kernel.{Concurrent, Ref, Resource}
import cats.syntax.all.*
import cats.~>
import org.typelevel.log4cats.{StructuredLogger, LoggerKernel, KernelLogLevel}
import org.typelevel.log4cats.Log

/**
 * `StructuredLogger` that does not immediately log.
 *
 * Similar in idea to `WriterStructuredLogger`, but a bit safer. This will not lose logs when the
 * effect fails, instead logging when the resource is cancelled or fails.
 *
 * This can be used to implement failure-only logging.
 * {{{
 *   def handleRequest[F[_](request: Request[F], logger: StructuredLogger[F]): OptionT[F, Response[F]] = ???
 *
 *   HttpRoutes[F] { req =>
 *     DeferredStructuredLogger[F](logger)
 *       .mapK(OptionT.liftK[F])
 *       .use { logger =>
 *         handleRequest(request, deferredLogger).flatTap { response =>
 *           deferredLogger.log.unlessA(response.status.isSuccess)
 *         }
 *       }
 *   }
 * }}}
 *
 * >>> WARNING: READ BEFORE USAGE! <<<
 * https://github.com/typelevel/log4cats/blob/main/core/shared/src/main/scala/org/typelevel/log4cats/extras/README.md
 * >>> WARNING: READ BEFORE USAGE! <<<
 */
trait DeferredStructuredLogger[F[_]] extends StructuredLogger[F] with DeferredLogging[F] {
  protected def kernel: LoggerKernel[F, String]
  
  override def mapK[G[_]](fk: F ~> G): DeferredStructuredLogger[G] =
    DeferredStructuredLogger.mapK(this, fk)

  override def addContext(ctx: Map[String, String]): DeferredStructuredLogger[F] =
    DeferredStructuredLogger.withContext(this, ctx)

  override def addContext(
      pairs: (String, Shown)*
  ): DeferredStructuredLogger[F] =
    DeferredStructuredLogger.withContext(this, pairs.map { case (k, v) => (k, v.toString) }.toMap)

  override def withModifiedString(f: String => String): DeferredStructuredLogger[F] =
    DeferredStructuredLogger.withModifiedString[F](this, f)
}
object DeferredStructuredLogger {
  def apply[F[_]](
      logger: StructuredLogger[F]
  )(implicit F: Concurrent[F]): Resource[F, DeferredStructuredLogger[F]] =
    Resource
      .makeCase(Ref.empty[F, Chain[DeferredLogMessage]]) { (ref, exitCase) =>
        exitCase match {
          case ExitCase.Succeeded => F.unit
          case _ => ref.get.flatMap(_.traverse_(_.logStructured(logger)))
        }
      }
      .map { ref =>
        new DeferredStructuredLogger[F] {
          private def save(lm: DeferredLogMessage): F[Unit] = ref.update(_.append(lm))

          protected def kernel: LoggerKernel[F, String] = new LoggerKernel[F, String] {
            def log(level: KernelLogLevel, logBuilder: Log.Builder[String] => Log.Builder[String]): F[Unit] = {
              val log = logBuilder(Log.mutableBuilder[String]()).build()
              val logLevel = level match {
                case KernelLogLevel.Trace => LogLevel.Trace
                case KernelLogLevel.Debug => LogLevel.Debug
                case KernelLogLevel.Info => LogLevel.Info
                case KernelLogLevel.Warn => LogLevel.Warn
                case KernelLogLevel.Error => LogLevel.Error
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

          override def trace(ctx: Map[String, String])(msg: => String): F[Unit] =
            save(DeferredLogMessage.trace(ctx, none, () => msg))
          override def debug(ctx: Map[String, String])(msg: => String): F[Unit] =
            save(DeferredLogMessage.debug(ctx, none, () => msg))
          override def info(ctx: Map[String, String])(msg: => String): F[Unit] =
            save(DeferredLogMessage.info(ctx, none, () => msg))
          override def warn(ctx: Map[String, String])(msg: => String): F[Unit] =
            save(DeferredLogMessage.warn(ctx, none, () => msg))
          override def error(ctx: Map[String, String])(msg: => String): F[Unit] =
            save(DeferredLogMessage.error(ctx, none, () => msg))

          override def trace(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
            save(DeferredLogMessage.trace(ctx, t.some, () => msg))
          override def debug(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
            save(DeferredLogMessage.debug(ctx, t.some, () => msg))
          override def info(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
            save(DeferredLogMessage.info(ctx, t.some, () => msg))
          override def warn(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
            save(DeferredLogMessage.warn(ctx, t.some, () => msg))
          override def error(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
            save(DeferredLogMessage.error(ctx, t.some, () => msg))

          override def trace(t: Throwable)(msg: => String): F[Unit] =
            save(DeferredLogMessage.trace(Map.empty, t.some, () => msg))
          override def debug(t: Throwable)(msg: => String): F[Unit] =
            save(DeferredLogMessage.debug(Map.empty, t.some, () => msg))
          override def info(t: Throwable)(msg: => String): F[Unit] =
            save(DeferredLogMessage.info(Map.empty, t.some, () => msg))
          override def warn(t: Throwable)(msg: => String): F[Unit] =
            save(DeferredLogMessage.warn(Map.empty, t.some, () => msg))
          override def error(t: Throwable)(msg: => String): F[Unit] =
            save(DeferredLogMessage.error(Map.empty, t.some, () => msg))

          override def trace(msg: => String): F[Unit] =
            save(DeferredLogMessage.trace(Map.empty, none, () => msg))
          override def debug(msg: => String): F[Unit] =
            save(DeferredLogMessage.debug(Map.empty, none, () => msg))
          override def info(msg: => String): F[Unit] =
            save(DeferredLogMessage.info(Map.empty, none, () => msg))
          override def warn(msg: => String): F[Unit] =
            save(DeferredLogMessage.warn(Map.empty, none, () => msg))
          override def error(msg: => String): F[Unit] =
            save(DeferredLogMessage.error(Map.empty, none, () => msg))

          override def inspect: F[Chain[DeferredLogMessage]] = ref.get

          override def log: F[Unit] =
            ref.getAndSet(Chain.empty).flatMap(_.traverse_(_.logStructured(logger)))
        }
      }

  private def mapK[F[_], G[_]](
      logger: DeferredStructuredLogger[F],
      fk: F ~> G
  ): DeferredStructuredLogger[G] =
    new DeferredStructuredLogger[G] {
      protected def kernel: LoggerKernel[G, String] = logger.kernel.mapK(fk)
      
      override def inspect: G[Chain[DeferredLogMessage]] = fk(logger.inspect)
      override def log: G[Unit] = fk(logger.log)

      override def trace(ctx: Map[String, String], t: Throwable)(msg: => String): G[Unit] =
        fk(logger.trace(ctx, t)(msg))
      override def debug(ctx: Map[String, String], t: Throwable)(msg: => String): G[Unit] =
        fk(logger.debug(ctx, t)(msg))
      override def info(ctx: Map[String, String], t: Throwable)(msg: => String): G[Unit] =
        fk(logger.info(ctx, t)(msg))
      override def warn(ctx: Map[String, String], t: Throwable)(msg: => String): G[Unit] =
        fk(logger.warn(ctx, t)(msg))
      override def error(ctx: Map[String, String], t: Throwable)(msg: => String): G[Unit] =
        fk(logger.error(ctx, t)(msg))

      override def trace(ctx: Map[String, String])(msg: => String): G[Unit] =
        fk(logger.trace(ctx)(msg))
      override def debug(ctx: Map[String, String])(msg: => String): G[Unit] =
        fk(logger.debug(ctx)(msg))
      override def info(ctx: Map[String, String])(msg: => String): G[Unit] =
        fk(logger.info(ctx)(msg))
      override def warn(ctx: Map[String, String])(msg: => String): G[Unit] =
        fk(logger.warn(ctx)(msg))
      override def error(ctx: Map[String, String])(msg: => String): G[Unit] =
        fk(logger.error(ctx)(msg))

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
      logger: DeferredStructuredLogger[F],
      baseCtx: Map[String, String]
  ): DeferredStructuredLogger[F] =
    new DeferredStructuredLogger[F] {
      protected def kernel: LoggerKernel[F, String] = logger.kernel
      
      private def addCtx(ctx: Map[String, String]): Map[String, String] = baseCtx ++ ctx

      override def inspect: F[Chain[DeferredLogMessage]] = logger.inspect
      override def log: F[Unit] = logger.log

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
      logger: DeferredStructuredLogger[F],
      f: String => String
  ): DeferredStructuredLogger[F] =
    new DeferredStructuredLogger[F] {
      protected def kernel: LoggerKernel[F, String] = logger.kernel
      
      override def inspect: F[Chain[DeferredLogMessage]] = logger.inspect
      override def log: F[Unit] = logger.log

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
