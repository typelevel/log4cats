package io.chrisdavenport.log4cats.testing

import io.chrisdavenport.log4cats.Logger
import cats.effect.Sync
import cats.implicits._
import java.util.concurrent.atomic.AtomicReference
import scala.annotation.tailrec

trait TestingLogger[F[_]] extends Logger[F] {
  import TestingLogger.LogMessage
  def logged: F[Vector[LogMessage]]
}

object TestingLogger {
  sealed trait LogMessage
  final case class TRACE(message: String, throwOpt: Option[Throwable]) extends LogMessage
  final case class DEBUG(message: String, throwOpt: Option[Throwable]) extends LogMessage
  final case class INFO(message: String, throwOpt: Option[Throwable]) extends LogMessage
  final case class WARN(message: String, throwOpt: Option[Throwable]) extends LogMessage
  final case class ERROR(message: String, throwOpt: Option[Throwable]) extends LogMessage

  def impl[F[_]: Sync](
    traceEnabled: Boolean = true,
    debugEnabled: Boolean = true,
    infoEnabled: Boolean = true,
    warnEnabled: Boolean = true,
    errorEnabled: Boolean = true
  ): TestingLogger[F] = {
    val ar = new AtomicReference(Vector.empty[LogMessage])
    def appendLogMessage(m: LogMessage): F[Unit] = Sync[F].delay{
      @tailrec
      def mod(): Unit = {
        val c = ar.get
        val u = c :+ m
        if (!ar.compareAndSet(c, u)) mod
        else ()
      }
      mod()
    }

    new TestingLogger[F]{
      def logged: F[Vector[LogMessage]] = Sync[F].delay(ar.get)

      def isTraceEnabled: F[Boolean] = Sync[F].pure(traceEnabled)
      def isDebugEnabled: F[Boolean] = Sync[F].pure(debugEnabled)
      def isInfoEnabled: F[Boolean] = Sync[F].pure(infoEnabled)
      def isWarnEnabled: F[Boolean] = Sync[F].pure(warnEnabled)
      def isErrorEnabled: F[Boolean] = Sync[F].pure(errorEnabled)

      def error(message: => String): F[Unit] =
        if (errorEnabled) appendLogMessage(ERROR(message, None)) else Sync[F].pure(())
      def error(t: Throwable)(message: => String): F[Unit] = 
        if (errorEnabled) appendLogMessage(ERROR(message, t.some)) else Sync[F].pure(())

      def warn(message: => String): F[Unit] = 
        if (warnEnabled) appendLogMessage(WARN(message, None)) else Sync[F].pure(())
      def warn(t: Throwable)(message: => String): F[Unit] = 
        if (warnEnabled) appendLogMessage(WARN(message, t.some)) else Sync[F].pure(())

      def info(message: => String): F[Unit] = 
        if (infoEnabled) appendLogMessage(INFO(message, None)) else Sync[F].pure(())
      def info(t: Throwable)(message: => String): F[Unit] = 
        if (infoEnabled) appendLogMessage(INFO(message, t.some)) else Sync[F].pure(())

      def debug(message: => String): F[Unit] = 
        if (debugEnabled) appendLogMessage(DEBUG(message, None)) else Sync[F].pure(())
      def debug(t: Throwable)(message: => String): F[Unit] = 
        if (debugEnabled) appendLogMessage(DEBUG(message, t.some)) else Sync[F].pure(())

      def trace(message: => String): F[Unit] = 
        if (traceEnabled) appendLogMessage(TRACE(message, None)) else Sync[F].pure(())
      def trace(t: Throwable)(message: => String): F[Unit] =
        if (traceEnabled) appendLogMessage(TRACE(message, t.some)) else Sync[F].pure(())
    }
  }

}