package io.chrisdavenport.log4cats.noop

import cats.Applicative
import io.chrisdavenport.log4cats.SelfAwareStructuredLogger

object NoOpLogger {
  def impl[F[_]: Applicative]: SelfAwareStructuredLogger[F] = new SelfAwareStructuredLogger[F] {

      val no = Applicative[F].pure(false)
      val unit = Applicative[F].pure(())
      @inline override def isTraceEnabled: F[Boolean] = no
      @inline override def isDebugEnabled: F[Boolean] = no
      @inline override def isInfoEnabled: F[Boolean] = no
      @inline override def isWarnEnabled: F[Boolean] = no
      @inline override def isErrorEnabled: F[Boolean] = no
      @inline override def trace(t: Throwable)(msg: => String): F[Unit] = unit
      @inline override def trace(msg: => String): F[Unit] = unit
      @inline override def trace(ctx: Map[String, String])(msg: => String): F[Unit] = unit
      @inline override def debug(t: Throwable)(msg: => String): F[Unit] = unit
      @inline override def debug(msg: => String): F[Unit] = unit
      @inline override def debug(ctx: Map[String, String])(msg: => String): F[Unit] = unit
      @inline override def info(t: Throwable)(msg: => String): F[Unit] = unit
      @inline override def info(msg: => String): F[Unit] = unit
      @inline override def info(ctx: Map[String, String])(msg: => String): F[Unit] = unit
      @inline override def warn(t: Throwable)(msg: => String): F[Unit] = unit
      @inline override def warn(msg: => String): F[Unit] = unit
      @inline override def warn(ctx: Map[String, String])(msg: => String): F[Unit] = unit
      @inline override def error(t: Throwable)(msg: => String): F[Unit] = unit
      @inline override def error(msg: => String): F[Unit] = unit
      @inline override def error(ctx: Map[String, String])(msg: => String): F[Unit] = unit
  }
}