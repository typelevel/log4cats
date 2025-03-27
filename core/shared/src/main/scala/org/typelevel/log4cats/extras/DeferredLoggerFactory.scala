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
import cats.effect.kernel.{Concurrent, Resource}
import cats.syntax.all.*
import cats.{~>, Functor}
import org.typelevel.log4cats.{LoggerFactory, SelfAwareStructuredLogger}

/**
 * A `LoggerFactory` that does not immediately log.
 *
 * Effectively a `LoggerFactory` equivalent to `DeferredSelfAwareStructuredLogger`. As an
 * implementation note, the `LoggerFactory` trait is constrained in such a way that this will
 * produce `SelfAwareStructuredLogger`, rather than `DeferredSelfAwareStructuredLogger`, so if
 * logging is desired it needs to be triggered using the `DeferredLoggerFactory`, rather than being
 * able to trigger it from any of the produced loggers.
 *
 * >>> WARNING: READ BEFORE USAGE! <<<
 * https://github.com/typelevel/log4cats/blob/main/core/shared/src/main/scala/org/typelevel/log4cats/extras/README.md
 * >>> WARNING: READ BEFORE USAGE! <<<
 */
trait DeferredLoggerFactory[F[_]] extends LoggerFactory[F] with DeferredLogging[F] {
  override def getLoggerFromName(name: String): SelfAwareStructuredLogger[F]

  override def addContext(ctx: Map[String, String])(implicit
      F: Functor[F]
  ): DeferredLoggerFactory[F] =
    DeferredLoggerFactory.addContext(this, ctx)

  override def addContext(pairs: (String, Shown)*)(implicit
      F: Functor[F]
  ): DeferredLoggerFactory[F] =
    DeferredLoggerFactory.addContext(this, pairs.map { case (k, v) => (k, v.toString) }.toMap)

  override def withModifiedString(f: String => String)(implicit
      F: Functor[F]
  ): DeferredLoggerFactory[F] =
    DeferredLoggerFactory.withModifiedString(this, f)

  override def mapK[G[_]](fk: F ~> G)(implicit F: Functor[F]): DeferredLoggerFactory[G] =
    DeferredLoggerFactory.mapK[F, G](fk)(this)
}
object DeferredLoggerFactory {

  def apply[F[_]: Concurrent](
      loggerFactory: LoggerFactory[F]
  ): Resource[F, DeferredLoggerFactory[F]] =
    DeferredSelfAwareStructuredLogger.makeCache[F].map { cache =>
      new DeferredLoggerFactory[F] {
        override def inspect: F[Chain[DeferredLogMessage]] = cache.get.map(_._1F)

        override def log: F[Unit] = {
          cache
            .getAndSet(Chain.empty)
            .flatMap(_.traverse_ { case (msg, logger) =>
              msg.logStructured(logger)
            })
        }

        override def fromName(name: String): F[SelfAwareStructuredLogger[F]] =
          loggerFactory.fromName(name).map(DeferredSelfAwareStructuredLogger(_, cache))

        override def getLoggerFromName(name: String): SelfAwareStructuredLogger[F] =
          DeferredSelfAwareStructuredLogger(loggerFactory.getLoggerFromName(name), cache)
      }
    }

  private def mapK[F[_]: Functor, G[_]](
      fk: F ~> G
  )(lf: DeferredLoggerFactory[F]): DeferredLoggerFactory[G] =
    new DeferredLoggerFactory[G] {
      override def inspect: G[Chain[DeferredLogMessage]] = fk(
        lf.inspect
      )
      override def log: G[Unit] = fk(lf.log)

      override def getLoggerFromName(name: String): SelfAwareStructuredLogger[G] =
        lf.getLoggerFromName(name).mapK(fk)

      override def fromName(name: String): G[SelfAwareStructuredLogger[G]] = fk(
        lf.fromName(name).map(_.mapK(fk))
      )
    }

  private def addContext[F[_]: Functor](
      lf: DeferredLoggerFactory[F],
      ctx: Map[String, String]
  ): DeferredLoggerFactory[F] =
    new DeferredLoggerFactory[F] {
      override def inspect: F[Chain[DeferredLogMessage]] = lf.inspect
      override def log: F[Unit] = lf.log

      override def getLoggerFromName(name: String): SelfAwareStructuredLogger[F] =
        lf.getLoggerFromName(name).addContext(ctx)

      override def fromName(name: String): F[SelfAwareStructuredLogger[F]] =
        lf.fromName(name).map(_.addContext(ctx))
    }

  private def withModifiedString[F[_]: Functor](
      lf: DeferredLoggerFactory[F],
      f: String => String
  ): DeferredLoggerFactory[F] =
    new DeferredLoggerFactory[F] {
      override def inspect: F[Chain[DeferredLogMessage]] = lf.inspect
      override def log: F[Unit] = lf.log

      override def getLoggerFromName(name: String): SelfAwareStructuredLogger[F] =
        lf.getLoggerFromName(name).withModifiedString(f)

      override def fromName(name: String): F[SelfAwareStructuredLogger[F]] =
        lf.fromName(name).map(_.withModifiedString(f))
    }

}
