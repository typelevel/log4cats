package io.chrisdavenport.log4cats.extras

import cats._
import cats.data._
import cats.syntax.all._
import io.chrisdavenport.log4cats._

object WriterLogger {

  def apply[G[_]: Alternative](
      traceEnabled: Boolean = true,
      debugEnabled: Boolean = true,
      infoEnabled: Boolean = true,
      warnEnabled: Boolean = true,
      errorEnabled: Boolean = true
  ): SelfAwareLogger[Writer[G[LogMessage], ?]] = {
    implicit val monoidGLogMessage = Alternative[G].algebra[LogMessage]
    new SelfAwareLogger[Writer[G[LogMessage], ?]] {
      def isTraceEnabled: Writer[G[LogMessage], Boolean] =
        Writer.value[G[LogMessage], Boolean](traceEnabled)
      def isDebugEnabled: Writer[G[LogMessage], Boolean] =
        Writer.value[G[LogMessage], Boolean](debugEnabled)
      def isInfoEnabled: Writer[G[LogMessage], Boolean] =
        Writer.value[G[LogMessage], Boolean](infoEnabled)
      def isWarnEnabled: Writer[G[LogMessage], Boolean] =
        Writer.value[G[LogMessage], Boolean](warnEnabled)
      def isErrorEnabled: Writer[G[LogMessage], Boolean] =
        Writer.value[G[LogMessage], Boolean](errorEnabled)

      def debug(t: Throwable)(message: => String): Writer[G[LogMessage], Unit] =
        if (debugEnabled)
          Writer.tell(Alternative[G].pure(LogMessage(LogLevel.Debug, t.some, message)))
        else Writer.value[G[LogMessage], Unit](())
      def error(t: Throwable)(message: => String): Writer[G[LogMessage], Unit] =
        if (errorEnabled)
          Writer.tell(Alternative[G].pure(LogMessage(LogLevel.Error, t.some, message)))
        else Writer.value[G[LogMessage], Unit](())
      def info(t: Throwable)(message: => String): Writer[G[LogMessage], Unit] =
        if (infoEnabled)
          Writer.tell(Alternative[G].pure(LogMessage(LogLevel.Info, t.some, message)))
        else Writer.value[G[LogMessage], Unit](())
      def trace(t: Throwable)(message: => String): Writer[G[LogMessage], Unit] =
        if (traceEnabled)
          Writer.tell(Alternative[G].pure(LogMessage(LogLevel.Trace, t.some, message)))
        else Writer.value[G[LogMessage], Unit](())
      def warn(t: Throwable)(message: => String): Writer[G[LogMessage], Unit] =
        if (warnEnabled)
          Writer.tell(Alternative[G].pure(LogMessage(LogLevel.Warn, t.some, message)))
        else Writer.value[G[LogMessage], Unit](())
      def debug(message: => String): Writer[G[LogMessage], Unit] =
        if (debugEnabled)
          Writer.tell(Alternative[G].pure(LogMessage(LogLevel.Debug, None, message)))
        else Writer.value[G[LogMessage], Unit](())
      def error(message: => String): Writer[G[LogMessage], Unit] =
        if (errorEnabled)
          Writer.tell(Alternative[G].pure(LogMessage(LogLevel.Error, None, message)))
        else Writer.value[G[LogMessage], Unit](())
      def info(message: => String): Writer[G[LogMessage], Unit] =
        if (infoEnabled) Writer.tell(Alternative[G].pure(LogMessage(LogLevel.Info, None, message)))
        else Writer.value[G[LogMessage], Unit](())
      def trace(message: => String): Writer[G[LogMessage], Unit] =
        if (traceEnabled)
          Writer.tell(Alternative[G].pure(LogMessage(LogLevel.Trace, None, message)))
        else Writer.value[G[LogMessage], Unit](())
      def warn(message: => String): Writer[G[LogMessage], Unit] =
        if (warnEnabled) Writer.tell(Alternative[G].pure(LogMessage(LogLevel.Warn, None, message)))
        else Writer.value[G[LogMessage], Unit](())
    }
  }

  def run[F[_]: Applicative, G[_]: Foldable](l: Logger[F]): Writer[G[LogMessage], ?] ~> F =
    new ~>[Writer[G[LogMessage], ?], F] {
      def logMessage(logMessage: LogMessage): F[Unit] = logMessage match {
        case LogMessage(LogLevel.Error, Some(t), m) =>
          l.error(t)(m)
        case LogMessage(LogLevel.Error, None, m) =>
          l.error(m)
        case LogMessage(LogLevel.Warn, Some(t), m) =>
          l.warn(t)(m)
        case LogMessage(LogLevel.Warn, None, m) =>
          l.warn(m)
        case LogMessage(LogLevel.Info, Some(t), m) =>
          l.info(t)(m)
        case LogMessage(LogLevel.Info, None, m) =>
          l.info(m)
        case LogMessage(LogLevel.Debug, Some(t), m) =>
          l.debug(t)(m)
        case LogMessage(LogLevel.Debug, None, m) =>
          l.debug(m)
        case LogMessage(LogLevel.Trace, Some(t), m) =>
          l.trace(t)(m)
        case LogMessage(LogLevel.Trace, None, m) =>
          l.trace(m)
      }

      def apply[A](fa: Writer[G[LogMessage], A]): F[A] = {
        val (toLog, out) = fa.run
        toLog.traverse_(logMessage).as(out)
      }
    }
}
