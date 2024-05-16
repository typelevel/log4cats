package org.typelevel.log4cats.extras

import cats.Show.Shown
import cats.data.Chain
import cats.effect.kernel.{Concurrent, Resource}
import cats.syntax.all.*
import cats.{Functor, ~>}
import org.typelevel.log4cats.extras.DeferredStructuredLogger.DeferredStructuredLogMessage
import org.typelevel.log4cats.{LoggerFactory, SelfAwareStructuredLogger}

trait DeferredLoggerFactory[F[_]] extends LoggerFactory[F] {
  def inspect: F[Chain[DeferredStructuredLogMessage]]
  def log: F[Unit]

  override def getLoggerFromName(name: String): SelfAwareStructuredLogger[F]

  override def addContext(ctx: Map[String, String])(implicit F: Functor[F]): DeferredLoggerFactory[F] =
    DeferredLoggerFactory.addContext(this, ctx)

  override def addContext(pairs: (String, Shown)*)(implicit F: Functor[F]): DeferredLoggerFactory[F] =
    DeferredLoggerFactory.addContext(this, pairs.map { case (k, v) => (k, v.toString) }.toMap)

  override def withModifiedString(f: String => String)(implicit F: Functor[F]): DeferredLoggerFactory[F] =
    DeferredLoggerFactory.withModifiedString(this, f)

  override def mapK[G[_]](fk: F ~> G)(implicit F: Functor[F]): DeferredLoggerFactory[G] =
    DeferredLoggerFactory.mapK[F, G](fk)(this)
}
object DeferredLoggerFactory {

  def apply[F[_]: Concurrent](loggerFactory: LoggerFactory[F]): Resource[F, DeferredLoggerFactory[F]] =
    DeferredSelfAwareStructuredLogger.makeCache[F].map { cache =>
      new DeferredLoggerFactory[F] {
        override def inspect: F[Chain[DeferredStructuredLogMessage]] = cache.get.map(_._1F)

        override def log: F[Unit] = {
          cache.getAndSet(Chain.empty).flatMap(_.traverse_ {
            case (msg, logger) => msg.log(logger)
          })
        }

        override def fromName(name: String): F[SelfAwareStructuredLogger[F]] =
          loggerFactory.fromName(name).map(DeferredSelfAwareStructuredLogger(_, cache))

        override def getLoggerFromName(name: String): SelfAwareStructuredLogger[F] =
          DeferredSelfAwareStructuredLogger(loggerFactory.getLoggerFromName(name), cache)
      }
    }

  private def mapK[F[_]: Functor, G[_]](fk: F ~> G)(lf: DeferredLoggerFactory[F]): DeferredLoggerFactory[G] =
    new DeferredLoggerFactory[G] {
      override def inspect: G[Chain[DeferredStructuredLogger.DeferredStructuredLogMessage]] = fk(lf.inspect)
      override def log: G[Unit] = fk(lf.log)

      override def getLoggerFromName(name: String): SelfAwareStructuredLogger[G] = lf.getLoggerFromName(name).mapK(fk)

      override def fromName(name: String): G[SelfAwareStructuredLogger[G]] = fk(lf.fromName(name).map(_.mapK(fk)))
    }

  private def addContext[F[_]: Functor](
                                         lf: DeferredLoggerFactory[F],
                                         ctx: Map[String, String]
                                       ): DeferredLoggerFactory[F] =
    new DeferredLoggerFactory[F] {
      override def inspect: F[Chain[DeferredStructuredLogger.DeferredStructuredLogMessage]] = lf.inspect
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
      override def inspect: F[Chain[DeferredStructuredLogger.DeferredStructuredLogMessage]] = lf.inspect
      override def log: F[Unit] = lf.log

      override def getLoggerFromName(name: String): SelfAwareStructuredLogger[F] =
        lf.getLoggerFromName(name).withModifiedString(f)

      override def fromName(name: String): F[SelfAwareStructuredLogger[F]] =
        lf.fromName(name).map(_.withModifiedString(f))
    }

}
