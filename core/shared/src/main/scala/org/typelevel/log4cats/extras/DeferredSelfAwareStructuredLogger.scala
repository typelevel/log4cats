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
import org.typelevel.log4cats.SelfAwareStructuredLogger

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
      override def isEnabled(ll: LogLevel): F[Boolean] = logger.isEnabled(ll)

      private def save(lm: DeferredLogMessage): F[Unit] =
        stash.update(_.append(lm -> logger))

      override def inspect: F[Chain[DeferredLogMessage]] = stash.get.map(_._1F)

      override def log: F[Unit] = stash
        .getAndSet(Chain.empty)
        .flatMap(_.traverse_ { case (msg, logger) =>
          msg.logStructured(logger)
        })

      override def log(
          ll: LogLevel,
          ctx: Map[String, String],
          t: Throwable,
          msg: => String
      ): F[Unit] =
        isEnabled(ll).flatMap(save(DeferredLogMessage(ll, ctx, t.some, () => msg)).whenA(_))

      override def log(ll: LogLevel, ctx: Map[String, String], msg: => String): F[Unit] =
        isEnabled(ll).flatMap(save(DeferredLogMessage(ll, ctx, none, () => msg)).whenA(_))

      override def log(ll: LogLevel, t: Throwable, msg: => String): F[Unit] =
        isEnabled(ll).flatMap(save(DeferredLogMessage(ll, Map.empty, t.some, () => msg)).whenA(_))

      override def log(ll: LogLevel, msg: => String): F[Unit] =
        isEnabled(ll).flatMap(save(DeferredLogMessage(ll, Map.empty, none, () => msg)).whenA(_))
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
      override def isEnabled(ll: LogLevel): G[Boolean] = fk(logger.isEnabled(ll))

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
      logger: DeferredSelfAwareStructuredLogger[F]
  )(baseCtx: Map[String, String]): DeferredSelfAwareStructuredLogger[F] =
    new DeferredSelfAwareStructuredLogger[F] {
      override def isEnabled(ll: LogLevel): F[Boolean] = logger.isEnabled(ll)

      override def inspect: F[Chain[DeferredLogMessage]] = logger.inspect

      override def log: F[Unit] = logger.log

      private def addCtx(ctx: Map[String, String]): Map[String, String] = baseCtx ++ ctx

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
        logger.log(ll, t, msg)

      override def log(ll: LogLevel, msg: => String): F[Unit] =
        logger.log(ll, msg)
    }

  def withModifiedString[F[_]](
      logger: DeferredSelfAwareStructuredLogger[F],
      f: String => String
  ): DeferredSelfAwareStructuredLogger[F] =
    new DeferredSelfAwareStructuredLogger[F] {
      override def isEnabled(ll: LogLevel): F[Boolean] = logger.isEnabled(ll)

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
