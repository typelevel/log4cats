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
import cats.syntax.all._
import cats.~>
import org.typelevel.log4cats.Logger

/**
 * `Logger` that does not immediately log.
 *
 * Similar in idea to `WriterLogger`, but a bit safer. This will not lose logs when the effect
 * fails, instead logging when the resource is cancelled or fails.
 *
 * This can be used to implement failure-only logging.
 * {{{
 *   def handleRequest[F[_](request: Request[F], logger: StructuredLogger[F]): OptionT[F, Response[F]] = ???
 *
 *   HttpRoutes[F] { req =>
 *     DeferredLogger[F](logger)
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
trait DeferredLogger[F[_]] extends Logger[F] with DeferredLogging[F] {
  override def withModifiedString(f: String => String): DeferredLogger[F] =
    DeferredLogger.withModifiedString(this, f)
  override def mapK[G[_]](fk: F ~> G): DeferredLogger[G] = DeferredLogger.mapK(this, fk)
}
object DeferredLogger {
  def apply[F[_]](logger: Logger[F])(implicit F: Concurrent[F]): Resource[F, DeferredLogger[F]] =
    Resource
      .makeCase(Ref.empty[F, Chain[DeferredLogMessage]]) { (ref, exitCase) =>
        exitCase match {
          case ExitCase.Succeeded => F.unit
          case _ => ref.get.flatMap(_.traverse_(_.log(logger)))
        }
      }
      .map { ref =>
        new DeferredLogger[F] {
          private def save(lm: DeferredLogMessage): F[Unit] = ref.update(_.append(lm))

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

          override def log: F[Unit] = ref.getAndSet(Chain.empty).flatMap(_.traverse_(_.log(logger)))
        }
      }

  private def mapK[F[_], G[_]](
      logger: DeferredLogger[F],
      fk: F ~> G
  ): DeferredLogger[G] =
    new DeferredLogger[G] {
      override def inspect: G[Chain[DeferredLogMessage]] = fk(logger.inspect)
      override def log: G[Unit] = fk(logger.log)

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

      override def withModifiedString(f: String => String): DeferredLogger[G] =
        DeferredLogger.withModifiedString(this, f)
      override def mapK[H[_]](fk: G ~> H): DeferredLogger[H] = DeferredLogger.mapK(this, fk)
    }

  def withModifiedString[F[_]](
      logger: DeferredLogger[F],
      f: String => String
  ): DeferredLogger[F] =
    new DeferredLogger[F] {
      override def inspect: F[Chain[DeferredLogMessage]] = logger.inspect
      override def log: F[Unit] = logger.log

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
