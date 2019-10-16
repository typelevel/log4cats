package io.chrisdavenport.log4cats.slf4j.internal

import io.chrisdavenport.log4cats._
import cats.implicits._
import cats.effect._
import cats.effect.implicits._
import org.slf4j.{Logger => JLogger}
import org.slf4j.MDC

private[slf4j] object Slf4jLoggerInternal {

  final val singletonsByName = true
  final val trailingDollar = false

  sealed trait LevelLogger[F[_]] extends Any {
    def isEnabled: F[Boolean]

    def apply(msg: => String): F[Unit]
    def apply(t: Throwable)(msg: => String): F[Unit]
  }

  private[this] def contextLog[F[_]](
      isEnabled: F[Boolean],
      ctx: Map[String, String],
      logging: F[Unit]
  )(implicit F: Sync[F]): F[Unit] =
    isEnabled.ifM(
      F.delay { MDC.getCopyOfContextMap }
        .bracket { _ =>
          F.delay {
            for {
              (k, v) <- ctx
            } MDC.put(k, v)
          } >> logging
        } { backup =>
          F.delay {
            if (backup eq null) MDC.clear()
            else MDC.setContextMap(backup)
          }
        },
      F.unit
    )

  final class Slf4jLogger[F[_]](val logger: JLogger)(implicit F: Sync[F])
      extends SelfAwareStructuredLogger[F] {
    override def isTraceEnabled: F[Boolean] = F.delay(logger.isTraceEnabled)
    override def isDebugEnabled: F[Boolean] = F.delay(logger.isDebugEnabled)
    override def isInfoEnabled: F[Boolean] = F.delay(logger.isInfoEnabled)
    override def isWarnEnabled: F[Boolean] = F.delay(logger.isWarnEnabled)
    override def isErrorEnabled: F[Boolean] = F.delay(logger.isErrorEnabled)

    override def trace(t: Throwable)(msg: => String): F[Unit] =
      isTraceEnabled
        .ifM(F.delay(logger.trace(msg, t)), F.unit)
    override def trace(msg: => String): F[Unit] =
      isTraceEnabled
        .ifM(F.delay(logger.trace(msg)), F.unit)
    override def trace(ctx: Map[String, String])(msg: => String): F[Unit] =
      contextLog(isTraceEnabled, ctx, F.delay(logger.trace(msg)))
    override def debug(t: Throwable)(msg: => String): F[Unit] =
      isDebugEnabled
        .ifM(F.delay(logger.debug(msg, t)), F.unit)
    override def debug(msg: => String): F[Unit] =
      isDebugEnabled
        .ifM(F.delay(logger.debug(msg)), F.unit)
    override def debug(ctx: Map[String, String])(msg: => String): F[Unit] =
      contextLog(isDebugEnabled, ctx, F.delay(logger.debug(msg)))
    override def info(t: Throwable)(msg: => String): F[Unit] =
      isInfoEnabled
        .ifM(F.delay(logger.info(msg, t)), F.unit)
    override def info(msg: => String): F[Unit] =
      isInfoEnabled
        .ifM(F.delay(logger.info(msg)), F.unit)
    override def info(ctx: Map[String, String])(msg: => String): F[Unit] =
      contextLog(isInfoEnabled, ctx, F.delay(logger.info(msg)))
    override def warn(t: Throwable)(msg: => String): F[Unit] =
      isWarnEnabled
        .ifM(F.delay(logger.warn(msg, t)), F.unit)
    override def warn(msg: => String): F[Unit] =
      isWarnEnabled
        .ifM(F.delay(logger.warn(msg)), F.unit)
    override def warn(ctx: Map[String, String])(msg: => String): F[Unit] =
      contextLog(isWarnEnabled, ctx, F.delay(logger.warn(msg)))
    override def error(t: Throwable)(msg: => String): F[Unit] =
      isErrorEnabled
        .ifM(F.delay(logger.error(msg, t)), F.unit)
    override def error(msg: => String): F[Unit] =
      isErrorEnabled
        .ifM(F.delay(logger.error(msg)), F.unit)
    override def error(ctx: Map[String, String])(msg: => String): F[Unit] =
      contextLog(isErrorEnabled, ctx, F.delay(logger.error(msg)))
    override def trace(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
      contextLog(isTraceEnabled, ctx, F.delay(logger.trace(msg, t)))
    override def debug(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
      contextLog(isDebugEnabled, ctx, F.delay(logger.debug(msg, t)))
    override def info(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
      contextLog(isInfoEnabled, ctx, F.delay(logger.info(msg, t)))
    override def warn(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
      contextLog(isWarnEnabled, ctx, F.delay(logger.warn(msg, t)))
    override def error(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
      contextLog(isErrorEnabled, ctx, F.delay(logger.error(msg, t)))
  }
}
