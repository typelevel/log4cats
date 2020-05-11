package io.chrisdavenport.log4cats.testing

import io.chrisdavenport.log4cats.SelfAwareStructuredLogger
import cats.effect.Sync
import cats.implicits._
import java.util.concurrent.atomic.AtomicReference

import scala.annotation.tailrec

trait StructuredTestingLogger[F[_]] extends SelfAwareStructuredLogger[F] {
  import StructuredTestingLogger.LogMessage
  def logged: F[Vector[LogMessage]]
}

object StructuredTestingLogger {

  sealed trait LogMessage {
    def ctx: Map[String, String]
    def message: String
    def throwOpt: Option[Throwable]
  }

  final case class TRACE(
      message: String,
      throwOpt: Option[Throwable],
      ctx: Map[String, String] = Map.empty
  ) extends LogMessage
  final case class DEBUG(
      message: String,
      throwOpt: Option[Throwable],
      ctx: Map[String, String] = Map.empty
  ) extends LogMessage
  final case class INFO(
      message: String,
      throwOpt: Option[Throwable],
      ctx: Map[String, String] = Map.empty
  ) extends LogMessage
  final case class WARN(
      message: String,
      throwOpt: Option[Throwable],
      ctx: Map[String, String] = Map.empty
  ) extends LogMessage
  final case class ERROR(
      message: String,
      throwOpt: Option[Throwable],
      ctx: Map[String, String] = Map.empty
  ) extends LogMessage

  def impl[F[_]: Sync](
      traceEnabled: Boolean = true,
      debugEnabled: Boolean = true,
      infoEnabled: Boolean = true,
      warnEnabled: Boolean = true,
      errorEnabled: Boolean = true
  ): StructuredTestingLogger[F] = {
    val ar = new AtomicReference(Vector.empty[LogMessage])
    def appendLogMessage(m: LogMessage): F[Unit] = Sync[F].delay {
      @tailrec
      def mod(): Unit = {
        val c = ar.get
        val u = c :+ m
        if (!ar.compareAndSet(c, u)) mod
        else ()
      }
      mod()
    }

    new StructuredTestingLogger[F] {
      def logged: F[Vector[LogMessage]] = Sync[F].delay(ar.get)

      def isTraceEnabled: F[Boolean] = Sync[F].pure(traceEnabled)
      def isDebugEnabled: F[Boolean] = Sync[F].pure(debugEnabled)
      def isInfoEnabled: F[Boolean] = Sync[F].pure(infoEnabled)
      def isWarnEnabled: F[Boolean] = Sync[F].pure(warnEnabled)
      def isErrorEnabled: F[Boolean] = Sync[F].pure(errorEnabled)

      private val noop = Sync[F].pure(())

      def error(message: => String): F[Unit] =
        if (errorEnabled) appendLogMessage(ERROR(message, None)) else noop
      def error(t: Throwable)(message: => String): F[Unit] =
        if (errorEnabled) appendLogMessage(ERROR(message, t.some)) else noop
      def error(ctx: Map[String, String])(message: => String): F[Unit] =
        if (errorEnabled) appendLogMessage(ERROR(message, None, ctx)) else noop
      def error(ctx: Map[String, String], t: Throwable)(message: => String): F[Unit] =
        if (errorEnabled) appendLogMessage(ERROR(message, t.some, ctx)) else noop

      def warn(message: => String): F[Unit] =
        if (warnEnabled) appendLogMessage(WARN(message, None)) else noop
      def warn(t: Throwable)(message: => String): F[Unit] =
        if (warnEnabled) appendLogMessage(WARN(message, t.some)) else noop
      def warn(ctx: Map[String, String])(message: => String): F[Unit] =
        if (warnEnabled) appendLogMessage(WARN(message, None, ctx)) else noop
      def warn(ctx: Map[String, String], t: Throwable)(message: => String): F[Unit] =
        if (warnEnabled) appendLogMessage(WARN(message, t.some, ctx)) else noop

      def info(message: => String): F[Unit] =
        if (infoEnabled) appendLogMessage(INFO(message, None)) else noop
      def info(t: Throwable)(message: => String): F[Unit] =
        if (infoEnabled) appendLogMessage(INFO(message, t.some)) else noop
      def info(ctx: Map[String, String])(message: => String): F[Unit] =
        if (infoEnabled) appendLogMessage(INFO(message, None, ctx)) else noop
      def info(ctx: Map[String, String], t: Throwable)(message: => String): F[Unit] =
        if (infoEnabled) appendLogMessage(INFO(message, t.some, ctx)) else noop

      def debug(message: => String): F[Unit] =
        if (debugEnabled) appendLogMessage(DEBUG(message, None)) else noop
      def debug(t: Throwable)(message: => String): F[Unit] =
        if (debugEnabled) appendLogMessage(DEBUG(message, t.some)) else noop
      def debug(ctx: Map[String, String])(message: => String): F[Unit] =
        if (debugEnabled) appendLogMessage(DEBUG(message, None, ctx)) else noop
      def debug(ctx: Map[String, String], t: Throwable)(message: => String): F[Unit] =
        if (debugEnabled) appendLogMessage(DEBUG(message, t.some, ctx)) else noop

      def trace(message: => String): F[Unit] =
        if (traceEnabled) appendLogMessage(TRACE(message, None)) else noop
      def trace(t: Throwable)(message: => String): F[Unit] =
        if (traceEnabled) appendLogMessage(TRACE(message, t.some)) else noop
      def trace(ctx: Map[String, String])(message: => String): F[Unit] =
        if (traceEnabled) appendLogMessage(TRACE(message, None, ctx)) else noop
      def trace(ctx: Map[String, String], t: Throwable)(message: => String): F[Unit] =
        if (traceEnabled) appendLogMessage(TRACE(message, t.some, ctx)) else noop
    }
  }

}
