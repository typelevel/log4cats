package io.chrisdavenport.log4cats.mtl

import cats.FlatMap
import cats.mtl.ApplicativeAsk
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.chrisdavenport.log4cats.SelfAwareStructuredLogger

object ApplicativeAskLogger {

  def unsafeCreate[F[_]: FlatMap: ApplicativeAsk[?[_], A], A: CtxEncoder](
      logger: SelfAwareStructuredLogger[F]
  ): SelfAwareStructuredLogger[F] =
    new ApplicativeAskLogger(logger)

  def create[F[_]: FlatMap: ApplicativeAsk[?[_], A], A: CtxEncoder](
      logger: F[SelfAwareStructuredLogger[F]]
  ): F[SelfAwareStructuredLogger[F]] =
    logger.map(v => new ApplicativeAskLogger(v))

}

final class ApplicativeAskLogger[F[_]: FlatMap: ApplicativeAsk[?[_], A], A: CtxEncoder](
    underlying: SelfAwareStructuredLogger[F]
) extends SelfAwareStructuredLogger[F] {

  override def trace(ctx: Map[String, String])(msg: => String): F[Unit] =
    withLocalCtx(local => underlying.trace(ctx ++ local)(msg))

  override def trace(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
    withLocalCtx(local => underlying.trace(ctx ++ local, t)(msg))

  override def debug(ctx: Map[String, String])(msg: => String): F[Unit] =
    withLocalCtx(local => underlying.debug(ctx ++ local)(msg))

  override def debug(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
    withLocalCtx(local => underlying.debug(ctx ++ local, t)(msg))

  override def info(ctx: Map[String, String])(msg: => String): F[Unit] =
    withLocalCtx(local => underlying.info(ctx ++ local)(msg))

  override def info(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
    withLocalCtx(local => underlying.info(ctx ++ local, t)(msg))

  override def warn(ctx: Map[String, String])(msg: => String): F[Unit] =
    withLocalCtx(local => underlying.warn(ctx ++ local)(msg))

  override def warn(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
    withLocalCtx(local => underlying.warn(ctx ++ local, t)(msg))

  override def error(ctx: Map[String, String])(msg: => String): F[Unit] =
    withLocalCtx(local => underlying.error(ctx ++ local)(msg))

  override def error(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
    withLocalCtx(local => underlying.error(ctx ++ local, t)(msg))

  override def isTraceEnabled: F[Boolean] = underlying.isTraceEnabled
  override def isDebugEnabled: F[Boolean] = underlying.isDebugEnabled
  override def isInfoEnabled: F[Boolean] = underlying.isInfoEnabled
  override def isWarnEnabled: F[Boolean] = underlying.isWarnEnabled
  override def isErrorEnabled: F[Boolean] = underlying.isErrorEnabled

  override def error(message: => String): F[Unit] =
    withLocalCtx(local => underlying.error(local)(message))

  override def warn(message: => String): F[Unit] =
    withLocalCtx(local => underlying.warn(local)(message))

  override def info(message: => String): F[Unit] =
    withLocalCtx(local => underlying.info(local)(message))

  override def debug(message: => String): F[Unit] =
    withLocalCtx(local => underlying.debug(local)(message))

  override def trace(message: => String): F[Unit] =
    withLocalCtx(local => underlying.trace(local)(message))

  override def error(t: Throwable)(message: => String): F[Unit] =
    withLocalCtx(local => underlying.error(local, t)(message))

  override def warn(t: Throwable)(message: => String): F[Unit] =
    withLocalCtx(local => underlying.warn(local, t)(message))

  override def info(t: Throwable)(message: => String): F[Unit] =
    withLocalCtx(local => underlying.info(local, t)(message))

  override def debug(t: Throwable)(message: => String): F[Unit] =
    withLocalCtx(local => underlying.debug(local, t)(message))

  override def trace(t: Throwable)(message: => String): F[Unit] =
    withLocalCtx(local => underlying.trace(local, t)(message))

  @inline private def withLocalCtx[B](f: Map[String, String] => F[B]): F[B] =
    ApplicativeAsk.ask.flatMap(a => f(CtxEncoder[A].encode(a)))
}

trait CtxEncoder[A] {
  def encode(value: A): Map[String, String]
}

object CtxEncoder {
  def apply[A](implicit ev: CtxEncoder[A]): CtxEncoder[A] = ev
}
