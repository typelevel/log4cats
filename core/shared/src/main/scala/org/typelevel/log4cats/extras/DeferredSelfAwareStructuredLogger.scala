package org.typelevel.log4cats.extras

import cats.data.Chain
import cats.effect.kernel.{Concurrent, Resource}
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.extras.DeferredStructuredLogger.DeferredStructuredLogMessage

/**
 * Similar to `DeferredStructuredLogger`, for `SelfAwareStructuredLogger`
 */
trait DeferredSelfAwareStructuredLogger[F[_]] extends SelfAwareStructuredLogger[F] {
  def inspect: F[Chain[DeferredStructuredLogMessage]]
  def log: F[Unit]
}
object DeferredSelfAwareStructuredLogger {
  def apply[F[_]](
      logger: SelfAwareStructuredLogger[F]
  )(implicit F: Concurrent[F]): Resource[F, DeferredSelfAwareStructuredLogger[F]] =
    DeferredStructuredLogger[F](logger).map { deferredLogger =>
      new DeferredSelfAwareStructuredLogger[F] {
        override def isTraceEnabled: F[Boolean] = logger.isTraceEnabled
        override def isDebugEnabled: F[Boolean] = logger.isDebugEnabled
        override def isInfoEnabled: F[Boolean] = logger.isInfoEnabled
        override def isWarnEnabled: F[Boolean] = logger.isWarnEnabled
        override def isErrorEnabled: F[Boolean] = logger.isErrorEnabled

        override def trace(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
          deferredLogger.trace(ctx, t)(msg)
        override def debug(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
          deferredLogger.debug(ctx, t)(msg)
        override def info(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
          deferredLogger.info(ctx, t)(msg)
        override def warn(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
          deferredLogger.warn(ctx, t)(msg)
        override def error(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
          deferredLogger.error(ctx, t)(msg)

        override def trace(ctx: Map[String, String])(msg: => String): F[Unit] =
          deferredLogger.trace(ctx)(msg)
        override def debug(ctx: Map[String, String])(msg: => String): F[Unit] =
          deferredLogger.debug(ctx)(msg)
        override def info(ctx: Map[String, String])(msg: => String): F[Unit] =
          deferredLogger.info(ctx)(msg)
        override def warn(ctx: Map[String, String])(msg: => String): F[Unit] =
          deferredLogger.warn(ctx)(msg)
        override def error(ctx: Map[String, String])(msg: => String): F[Unit] =
          deferredLogger.error(ctx)(msg)

        override def trace(t: Throwable)(message: => String): F[Unit] =
          deferredLogger.trace(t)(message)
        override def debug(t: Throwable)(message: => String): F[Unit] =
          deferredLogger.debug(t)(message)
        override def info(t: Throwable)(message: => String): F[Unit] =
          deferredLogger.info(t)(message)
        override def warn(t: Throwable)(message: => String): F[Unit] =
          deferredLogger.warn(t)(message)
        override def error(t: Throwable)(message: => String): F[Unit] =
          deferredLogger.error(t)(message)

        override def trace(message: => String): F[Unit] = deferredLogger.trace(message)
        override def debug(message: => String): F[Unit] = deferredLogger.debug(message)
        override def info(message: => String): F[Unit] = deferredLogger.info(message)
        override def warn(message: => String): F[Unit] = deferredLogger.warn(message)
        override def error(message: => String): F[Unit] = deferredLogger.error(message)

        override def inspect: F[Chain[DeferredStructuredLogMessage]] = deferredLogger.inspect
        override def log: F[Unit] = deferredLogger.log
      }
    }
}
