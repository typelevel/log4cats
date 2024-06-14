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
import cats.syntax.all._
import cats.~>
import org.typelevel.log4cats.StructuredLogger

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

          override def inspect: F[Chain[DeferredLogMessage]] = ref.get

          override def log: F[Unit] =
            ref.getAndSet(Chain.empty).flatMap(_.traverse_(_.logStructured(logger)))

          override def log(
              ll: LogLevel,
              ctx: Map[String, String],
              t: Throwable,
              msg: => String
          ): F[Unit] =
            save(DeferredLogMessage(ll, ctx, t.some, () => msg))

          override def log(ll: LogLevel, ctx: Map[String, String], msg: => String): F[Unit] =
            save(DeferredLogMessage(ll, ctx, none, () => msg))

          override def log(ll: LogLevel, t: Throwable, msg: => String): F[Unit] =
            save(DeferredLogMessage(ll, Map.empty, t.some, () => msg))

          override def log(ll: LogLevel, msg: => String): F[Unit] =
            save(DeferredLogMessage(ll, Map.empty, none, () => msg))
        }
      }

  private def mapK[F[_], G[_]](
      logger: DeferredStructuredLogger[F],
      fk: F ~> G
  ): DeferredStructuredLogger[G] =
    new DeferredStructuredLogger[G] {
      override def inspect: G[Chain[DeferredLogMessage]] = fk(logger.inspect)
      override def log: G[Unit] = fk(logger.log)

      override def log(
          ll: LogLevel,
          ctx: Map[String, String],
          t: Throwable,
          msg: => String
      ): G[Unit] =
        fk(logger.log(ll, ctx, t, msg))

      override def log(ll: LogLevel, ctx: Map[String, String], msg: => String): G[Unit] =
        fk(logger.log(ll, ctx, msg))

      override def log(ll: LogLevel, t: Throwable, msg: => String): G[Unit] = fk(
        logger.log(ll, t, msg)
      )

      override def log(ll: LogLevel, msg: => String): G[Unit] = fk(logger.log(ll, msg))
    }

  def withContext[F[_]](
      logger: DeferredStructuredLogger[F],
      baseCtx: Map[String, String]
  ): DeferredStructuredLogger[F] =
    new DeferredStructuredLogger[F] {
      private def addCtx(ctx: Map[String, String]): Map[String, String] = baseCtx ++ ctx

      override def inspect: F[Chain[DeferredLogMessage]] = logger.inspect

      override def log: F[Unit] = logger.log

      override def log(
          ll: LogLevel,
          ctx: Map[String, String],
          t: Throwable,
          msg: => String
      ): F[Unit] =
        logger.log(ll, addCtx(ctx), t, msg)

      override def log(ll: LogLevel, ctx: Map[String, String], msg: => String): F[Unit] =
        logger.log(ll, addCtx(ctx), msg)

      override def log(ll: LogLevel, t: Throwable, msg: => String): F[Unit] =
        logger.log(ll, baseCtx, t, msg)

      override def log(ll: LogLevel, msg: => String): F[Unit] =
        logger.log(ll, baseCtx, msg)
    }

  def withModifiedString[F[_]](
      logger: DeferredStructuredLogger[F],
      f: String => String
  ): DeferredStructuredLogger[F] =
    new DeferredStructuredLogger[F] {
      override def inspect: F[Chain[DeferredLogMessage]] = logger.inspect

      override def log: F[Unit] = logger.log

      override def log(
          ll: LogLevel,
          ctx: Map[String, String],
          t: Throwable,
          msg: => String
      ): F[Unit] = logger.log(ll, ctx, t, f(msg))

      override def log(ll: LogLevel, ctx: Map[String, String], msg: => String): F[Unit] =
        logger.log(ll, ctx, f(msg))

      override def log(ll: LogLevel, t: Throwable, msg: => String): F[Unit] =
        logger.log(ll, t, f(msg))

      override def log(ll: LogLevel, msg: => String): F[Unit] = logger.log(ll, f(msg))
    }
}
