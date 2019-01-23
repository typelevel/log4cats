package io.chrisdavenport.log4cats.slf4j.internal

import io.chrisdavenport.log4cats._
import cats._
import cats.implicits._
import cats.effect._
import org.slf4j.{Logger => JLogger}

private[slf4j] object Slf4jLoggerInternal {

  final val singletonsByName = true
  final val trailingDollar = false

  sealed trait LevelLogger[F[_]] extends Any {
    def isEnabled: F[Boolean]

    def apply(msg: => String): F[Unit]
    def apply(t: Throwable)(msg: => String): F[Unit]
  }

  class IntermediateForConcretes[F[_]: Applicative] extends SelfAwareStructuredLogger[F]{
      def isTraceEnabled: F[Boolean] = true.pure[F]
      def isDebugEnabled: F[Boolean] = true.pure[F]
      def isInfoEnabled: F[Boolean] = true.pure[F]
      def isWarnEnabled: F[Boolean] = true.pure[F]
      def isErrorEnabled: F[Boolean] = true.pure[F]

      override def trace(t: Throwable)(msg: => String): F[Unit] =  Applicative[F].unit
      override def trace(msg: => String): F[Unit] = Applicative[F].unit
      override def trace(ctx: Map[String, String])(msg: => String): F[Unit] = Applicative[F].unit
      override def debug(t: Throwable)(msg: => String): F[Unit] = Applicative[F].unit
      override def debug(msg: => String): F[Unit] = Applicative[F].unit
      override def debug(ctx: Map[String, String])(msg: => String): F[Unit] = Applicative[F].unit
      override def info(t: Throwable)(msg: => String): F[Unit] = Applicative[F].unit
      override def info(msg: => String): F[Unit] = Applicative[F].unit
      override def info(ctx: Map[String, String])(msg: => String): F[Unit] = Applicative[F].unit
      override def warn(t: Throwable)(msg: => String): F[Unit] = Applicative[F].unit
      override def warn(msg: => String): F[Unit] = Applicative[F].unit
      override def warn(ctx: Map[String, String])(msg: => String): F[Unit] = Applicative[F].unit
      override def error(t: Throwable)(msg: => String): F[Unit] = Applicative[F].unit
      override def error(msg: => String): F[Unit] = Applicative[F].unit
      override def error(ctx: Map[String, String])(msg: => String): F[Unit] = Applicative[F].unit
      override def trace(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
        Applicative[F].unit
      override def debug(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
        Applicative[F].unit
      override def info(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
        Applicative[F].unit
      override def warn(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
        Applicative[F].unit
      override def error(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
        Applicative[F].unit
  }
  
  class Slf4jLogger[F[_]: Sync](val logger: JLogger) extends IntermediateForConcretes[F] {
      override def isTraceEnabled: F[Boolean] = logger.isTraceEnabled.pure[F]
      override def isDebugEnabled: F[Boolean] = logger.isDebugEnabled.pure[F]
      override def isInfoEnabled: F[Boolean] = logger.isInfoEnabled.pure[F]
      override def isWarnEnabled: F[Boolean] = logger.isWarnEnabled.pure[F]
      override def isErrorEnabled: F[Boolean] = logger.isErrorEnabled.pure[F]

      override def trace(t: Throwable)(msg: => String): F[Unit] = macro ReflectiveLogMacros.traceTM[F]
      override def trace(msg: => String): F[Unit] = macro ReflectiveLogMacros.traceM[F]
      override def trace(ctx: Map[String, String])(msg: => String): F[Unit] = macro ReflectiveLogMacros.traceCM[F]
      override def debug(t: Throwable)(msg: => String): F[Unit] = macro ReflectiveLogMacros.debugTM[F]
      override def debug(msg: => String): F[Unit] = macro ReflectiveLogMacros.debugM[F]
      override def debug(ctx: Map[String, String])(msg: => String): F[Unit] = macro ReflectiveLogMacros.traceCM[F]
      override def info(t: Throwable)(msg: => String): F[Unit] = macro ReflectiveLogMacros.infoTM[F]
      override def info(msg: => String): F[Unit] = macro ReflectiveLogMacros.infoM[F]
      override def info(ctx: Map[String, String])(msg: => String): F[Unit] = macro ReflectiveLogMacros.infoCM[F]
      override def warn(t: Throwable)(msg: => String): F[Unit] = macro ReflectiveLogMacros.warnTM[F]
      override def warn(msg: => String): F[Unit] = macro ReflectiveLogMacros.warnM[F]
      override def warn(ctx: Map[String, String])(msg: => String): F[Unit] = macro ReflectiveLogMacros.warnCM[F]
      override def error(t: Throwable)(msg: => String): F[Unit] = macro ReflectiveLogMacros.errorTM[F]
      override def error(msg: => String): F[Unit] = macro ReflectiveLogMacros.errorM[F]
      override def error(ctx: Map[String, String])(msg: => String): F[Unit] = macro ReflectiveLogMacros.errorCM[F]
      override def trace(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
        macro ReflectiveLogMacros.traceCTM[F]
      override def debug(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
        macro ReflectiveLogMacros.debugCTM[F]
      override def info(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
        macro ReflectiveLogMacros.infoCTM[F]
      override def warn(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
        macro ReflectiveLogMacros.warnCTM[F]
      override def error(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
        macro ReflectiveLogMacros.errorCTM[F]
    }
}
