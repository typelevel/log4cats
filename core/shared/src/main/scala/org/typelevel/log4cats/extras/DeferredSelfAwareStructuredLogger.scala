package org.typelevel.log4cats.extras

import cats.data.Chain
import cats.effect.kernel.Resource.ExitCase
import cats.effect.kernel.{Concurrent, Ref, Resource}
import cats.syntax.all.*
import cats.{Show, ~>}
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.extras.DeferredStructuredLogger.{Debug, DeferredStructuredLogMessage, Error, Info, Trace, Warn}

/**
 * Similar to `DeferredStructuredLogger`, for `SelfAwareStructuredLogger`
 */
trait DeferredSelfAwareStructuredLogger[F[_]] extends SelfAwareStructuredLogger[F] {
  def inspect: F[Chain[DeferredStructuredLogMessage]]
  def log: F[Unit]

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
  def apply[F[_]: Concurrent](logger: SelfAwareStructuredLogger[F]): Resource[F, DeferredSelfAwareStructuredLogger[F]] =
    makeCache[F].map(apply[F](logger, _))

  def apply[F[_]: Concurrent](logger: SelfAwareStructuredLogger[F],
                              stash: Ref[F, Chain[(DeferredStructuredLogMessage, SelfAwareStructuredLogger[F])]]
                             ): DeferredSelfAwareStructuredLogger[F] =
    new DeferredSelfAwareStructuredLogger[F] {
      private def save(lm: DeferredStructuredLogMessage): F[Unit] = stash.update(_.append(lm -> logger))

      override def isTraceEnabled: F[Boolean] = logger.isTraceEnabled
      override def isDebugEnabled: F[Boolean] = logger.isDebugEnabled
      override def isInfoEnabled: F[Boolean] = logger.isInfoEnabled
      override def isWarnEnabled: F[Boolean] = logger.isWarnEnabled
      override def isErrorEnabled: F[Boolean] = logger.isErrorEnabled

      override def trace(ctx: Map[String, String])(msg: => String): F[Unit] = save(Trace(() => msg, none, ctx))
      override def debug(ctx: Map[String, String])(msg: => String): F[Unit] = save(Debug(() => msg, none, ctx))
      override def info(ctx: Map[String, String])(msg: => String): F[Unit] = save(Info(() => msg, none, ctx))
      override def warn(ctx: Map[String, String])(msg: => String): F[Unit] = save(Warn(() => msg, none, ctx))
      override def error(ctx: Map[String, String])(msg: => String): F[Unit] = save(Error(() => msg, none, ctx))

      override def trace(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] = save(Trace(() => msg, t.some, ctx))
      override def debug(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] = save(Debug(() => msg, t.some, ctx))
      override def info(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] = save(Info(() => msg, t.some, ctx))
      override def warn(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] = save(Warn(() => msg, t.some, ctx))
      override def error(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] = save(Error(() => msg, t.some, ctx))

      override def trace(t: Throwable)(msg: => String): F[Unit] = save(Trace(() => msg, t.some, Map.empty))
      override def debug(t: Throwable)(msg: => String): F[Unit] = save(Debug(() => msg, t.some, Map.empty))
      override def info(t: Throwable)(msg: => String): F[Unit] = save(Info(() => msg, t.some, Map.empty))
      override def warn(t: Throwable)(msg: => String): F[Unit] = save(Warn(() => msg, t.some, Map.empty))
      override def error(t: Throwable)(msg: => String): F[Unit] = save(Error(() => msg, t.some, Map.empty))

      override def trace(msg: => String): F[Unit] = save(Trace(() => msg, none, Map.empty))
      override def debug(msg: => String): F[Unit] = save(Debug(() => msg, none, Map.empty))
      override def info(msg: => String): F[Unit] = save(Info(() => msg, none, Map.empty))
      override def warn(msg: => String): F[Unit] = save(Warn(() => msg, none, Map.empty))
      override def error(msg: => String): F[Unit] = save(Error(() => msg, none, Map.empty))

      override def inspect: F[Chain[DeferredStructuredLogMessage]] = stash.get.map(_._1F)

      override def log: F[Unit] = stash.getAndSet(Chain.empty).flatMap(_.traverse_ {
        case (msg, logger) => msg.log(logger)
      })
    }

  private[extras] def makeCache[F[_]](implicit F: Concurrent[F])
  : Resource[F, Ref[F, Chain[(DeferredStructuredLogMessage, SelfAwareStructuredLogger[F])]]] =
    Resource
      .makeCase(Ref.empty[F, Chain[(DeferredStructuredLogMessage, SelfAwareStructuredLogger[F])]]) { (ref, exitCase) =>
        exitCase match {
          case ExitCase.Succeeded => F.unit
          case _ => ref.get.flatMap(_.traverse_ { case (msg, logger) => msg.log(logger) })
        }
      }

  private def mapK[F[_], G[_]](logger: DeferredSelfAwareStructuredLogger[F], fk: F ~> G)
  : DeferredSelfAwareStructuredLogger[G] =
    new DeferredSelfAwareStructuredLogger[G] {
      override def inspect: G[Chain[DeferredStructuredLogger.DeferredStructuredLogMessage]] = fk(logger.inspect)
      override def log: G[Unit] = fk(logger.log)
      override def isTraceEnabled: G[Boolean] = fk(logger.isTraceEnabled)
      override def isDebugEnabled: G[Boolean] = fk(logger.isDebugEnabled)
      override def isInfoEnabled: G[Boolean] = fk(logger.isInfoEnabled)
      override def isWarnEnabled: G[Boolean] = fk(logger.isWarnEnabled)
      override def isErrorEnabled: G[Boolean] = fk(logger.isErrorEnabled)

      override def trace(ctx: Map[String, String], t: Throwable)(msg: => String): G[Unit] = fk(logger.trace(ctx, t)(msg))
      override def debug(ctx: Map[String, String], t: Throwable)(msg: => String): G[Unit] = fk(logger.debug(ctx, t)(msg))
      override def info(ctx: Map[String, String], t: Throwable)(msg: => String): G[Unit] = fk(logger.info(ctx, t)(msg))
      override def warn(ctx: Map[String, String], t: Throwable)(msg: => String): G[Unit] = fk(logger.warn(ctx, t)(msg))
      override def error(ctx: Map[String, String], t: Throwable)(msg: => String): G[Unit] = fk(logger.error(ctx, t)(msg))

      override def trace(ctx: Map[String, String])(msg: => String): G[Unit] = fk(logger.trace(ctx)(msg))
      override def debug(ctx: Map[String, String])(msg: => String): G[Unit] = fk(logger.debug(ctx)(msg))
      override def info(ctx: Map[String, String])(msg: => String): G[Unit] = fk(logger.info(ctx)(msg))
      override def warn(ctx: Map[String, String])(msg: => String): G[Unit] = fk(logger.warn(ctx)(msg))
      override def error(ctx: Map[String, String])(msg: => String): G[Unit] = fk(logger.error(ctx)(msg))

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

  def withContext[F[_]](logger: DeferredSelfAwareStructuredLogger[F])(baseCtx: Map[String, String])
  : DeferredSelfAwareStructuredLogger[F] =
    new DeferredSelfAwareStructuredLogger[F] {
      private def addCtx(ctx: Map[String, String]): Map[String, String] = baseCtx ++ ctx

      override def inspect: F[Chain[DeferredStructuredLogger.DeferredStructuredLogMessage]] = logger.inspect
      override def log: F[Unit] = logger.log
      override def isTraceEnabled: F[Boolean] = logger.isTraceEnabled
      override def isDebugEnabled: F[Boolean] = logger.isDebugEnabled
      override def isInfoEnabled: F[Boolean] = logger.isInfoEnabled
      override def isWarnEnabled: F[Boolean] = logger.isWarnEnabled
      override def isErrorEnabled: F[Boolean] = logger.isErrorEnabled

      override def trace(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] = logger.trace(addCtx(ctx), t)(msg)
      override def debug(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] = logger.debug(addCtx(ctx), t)(msg)
      override def info(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] = logger.info(addCtx(ctx), t)(msg)
      override def warn(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] = logger.warn(addCtx(ctx), t)(msg)
      override def error(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] = logger.error(addCtx(ctx), t)(msg)

      override def trace(ctx: Map[String, String])(msg: => String): F[Unit] = logger.trace(addCtx(ctx))(msg)
      override def debug(ctx: Map[String, String])(msg: => String): F[Unit] = logger.debug(addCtx(ctx))(msg)
      override def info(ctx: Map[String, String])(msg: => String): F[Unit] = logger.info(addCtx(ctx))(msg)
      override def warn(ctx: Map[String, String])(msg: => String): F[Unit] = logger.warn(addCtx(ctx))(msg)
      override def error(ctx: Map[String, String])(msg: => String): F[Unit] = logger.error(addCtx(ctx))(msg)

      override def trace(t: Throwable)(message: => String): F[Unit] = logger.trace(baseCtx, t)(message)
      override def debug(t: Throwable)(message: => String): F[Unit] = logger.debug(baseCtx, t)(message)
      override def info(t: Throwable)(message: => String): F[Unit] = logger.info(baseCtx, t)(message)
      override def warn(t: Throwable)(message: => String): F[Unit] = logger.warn(baseCtx, t)(message)
      override def error(t: Throwable)(message: => String): F[Unit] = logger.error(baseCtx, t)(message)

      override def trace(message: => String): F[Unit] = logger.trace(baseCtx)(message)
      override def debug(message: => String): F[Unit] = logger.debug(baseCtx)(message)
      override def info(message: => String): F[Unit] = logger.info(baseCtx)(message)
      override def warn(message: => String): F[Unit] = logger.warn(baseCtx)(message)
      override def error(message: => String): F[Unit] = logger.error(baseCtx)(message)
    }

  def withModifiedString[F[_]](logger: DeferredSelfAwareStructuredLogger[F], f: String => String)
  : DeferredSelfAwareStructuredLogger[F] =
    new DeferredSelfAwareStructuredLogger[F] {
      override def inspect: F[Chain[DeferredStructuredLogger.DeferredStructuredLogMessage]] = logger.inspect
      override def log: F[Unit] = logger.log
      override def isTraceEnabled: F[Boolean] = logger.isTraceEnabled
      override def isDebugEnabled: F[Boolean] = logger.isDebugEnabled
      override def isInfoEnabled: F[Boolean] = logger.isInfoEnabled
      override def isWarnEnabled: F[Boolean] = logger.isWarnEnabled
      override def isErrorEnabled: F[Boolean] = logger.isErrorEnabled

      override def trace(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] = logger.trace(ctx, t)(f(msg))
      override def debug(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] = logger.debug(ctx, t)(f(msg))
      override def info(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] = logger.info(ctx, t)(f(msg))
      override def warn(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] = logger.warn(ctx, t)(f(msg))
      override def error(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] = logger.error(ctx, t)(f(msg))

      override def trace(ctx: Map[String, String])(msg: => String): F[Unit] = logger.trace(ctx)(f(msg))
      override def debug(ctx: Map[String, String])(msg: => String): F[Unit] = logger.debug(ctx)(f(msg))
      override def info(ctx: Map[String, String])(msg: => String): F[Unit] = logger.info(ctx)(f(msg))
      override def warn(ctx: Map[String, String])(msg: => String): F[Unit] = logger.warn(ctx)(f(msg))
      override def error(ctx: Map[String, String])(msg: => String): F[Unit] = logger.error(ctx)(f(msg))

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
