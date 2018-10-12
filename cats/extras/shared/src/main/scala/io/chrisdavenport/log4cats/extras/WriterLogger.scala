package io.chrisdavenport.log4cats.extras

import cats._
import cats.data._
import cats.implicits._
import io.chrisdavenport.log4cats._

object WriterLogger {

  def apply[G[_]: Alternative](
    traceEnabled: Boolean = true,
    debugEnabled: Boolean = true,
    infoEnabled: Boolean = true,
    warnEnabled: Boolean = true,
    errorEnabled: Boolean = true
  ): Logger[Writer[G[LogMessage], ?]] =  {
    implicit val monoidGLogMessage = Alternative[G].algebra[LogMessage]
    new Logger[Writer[G[LogMessage], ?]]{
      def debug(t: Throwable)(message: => String): Writer[G[LogMessage], Unit] =
        if (debugEnabled) Writer.tell(Alternative[G].pure(LogMessage(LogLevel.Debug, t.some, message)))
        else Writer.value[G[LogMessage], Unit](())
      def error(t: Throwable)(message: => String): Writer[G[LogMessage], Unit] = 
        if (errorEnabled) Writer.tell(Alternative[G].pure(LogMessage(LogLevel.Error, t.some, message)))
        else Writer.value[G[LogMessage], Unit](())
      def info(t: Throwable)(message: => String): Writer[G[LogMessage], Unit] =
        if (infoEnabled) Writer.tell(Alternative[G].pure(LogMessage(LogLevel.Info, t.some, message)))
        else Writer.value[G[LogMessage], Unit](())
      def trace(t: Throwable)(message: => String): Writer[G[LogMessage], Unit] = 
        if (traceEnabled) Writer.tell(Alternative[G].pure(LogMessage(LogLevel.Trace, t.some, message)))
        else Writer.value[G[LogMessage], Unit](())
      def warn(t: Throwable)(message: => String): Writer[G[LogMessage], Unit] = 
        if (warnEnabled) Writer.tell(Alternative[G].pure(LogMessage(LogLevel.Warn, t.some, message)))
        else Writer.value[G[LogMessage], Unit](())
      def debug(message: => String): Writer[G[LogMessage], Unit]  = 
        if (debugEnabled) Writer.tell(Alternative[G].pure(LogMessage(LogLevel.Debug, None, message)))
        else Writer.value[G[LogMessage], Unit](())
      def error(message: => String): Writer[G[LogMessage], Unit]  = 
        if (errorEnabled) Writer.tell(Alternative[G].pure(LogMessage(LogLevel.Error, None, message)))
        else Writer.value[G[LogMessage], Unit](())
      def info(message: => String): Writer[G[LogMessage], Unit] = 
        if (infoEnabled) Writer.tell(Alternative[G].pure(LogMessage(LogLevel.Info, None, message)))
        else Writer.value[G[LogMessage], Unit](())
      def trace(message: => String): Writer[G[LogMessage], Unit] = 
        if (traceEnabled) Writer.tell(Alternative[G].pure(LogMessage(LogLevel.Trace, None, message)))
        else Writer.value[G[LogMessage], Unit](())
      def warn(message: => String): Writer[G[LogMessage], Unit] = 
        if (warnEnabled) Writer.tell(Alternative[G].pure(LogMessage(LogLevel.Warn, None, message)))
        else Writer.value[G[LogMessage], Unit](())
    }
  }
}