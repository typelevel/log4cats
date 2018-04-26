package io.chrisdavenport.log4cats.scribe

import io.chrisdavenport.log4cats.Logger
import _root_.scribe.{Logger => Base,  Level, LogRecord}
import cats.effect.Sync

object ScribeLogger {

  def fromLogger[F[_]: Sync](logger: Base): Logger[F] = new Logger[F]{

    // This May Be a Very Bad Approach...
    def isTraceEnabled: F[Boolean] = 
      Sync[F].delay(
        logger.modifiers.find(mod => mod(LogRecord.simple("", "", "", None, None, Level.Trace)).isDefined).isDefined
      )
    def isDebugEnabled: F[Boolean] = 
      Sync[F].delay(
        logger.modifiers.find(mod => mod(LogRecord.simple("", "", "", None, None, Level.Debug)).isDefined).isDefined
      )
    def isInfoEnabled: F[Boolean] =
      Sync[F].delay(
        logger.modifiers.find(mod => mod(LogRecord.simple("", "", "", None, None, Level.Info)).isDefined).isDefined
      )
    def isWarnEnabled: F[Boolean] = 
      Sync[F].delay(
        logger.modifiers.find(mod => mod(LogRecord.simple("", "", "", None, None, Level.Warn)).isDefined).isDefined
      )
    def isErrorEnabled: F[Boolean] = 
      Sync[F].delay(
        logger.modifiers.find(mod => mod(LogRecord.simple("", "", "", None, None, Level.Error)).isDefined).isDefined
      )

    def error(message: => String): F[Unit] = 
      Sync[F].delay(logger.error(message))
    def error(t: Throwable)(message: => String): F[Unit] =
      Sync[F].delay(logger.error(message, t))
    def warn(message: => String): F[Unit] =
      Sync[F].delay(logger.warn(message))
    def warn(t: Throwable)(message: => String): F[Unit] =
      Sync[F].delay(logger.warn(message, t))
    def info(message: => String): F[Unit] = 
      Sync[F].delay(logger.info(message))
    def info(t: Throwable)(message: => String): F[Unit] =
      Sync[F].delay(logger.info(message, t))
    def debug(message: => String): F[Unit] =
      Sync[F].delay(logger.debug(message))
    def debug(t: Throwable)(message: => String): F[Unit] =
      Sync[F].delay(logger.debug(message, t))
    def trace(message: => String): F[Unit] =
      Sync[F].delay(logger.trace(message))
    def trace(t: Throwable)(message: => String): F[Unit] =
      Sync[F].delay(logger.trace(message, t))
  }

}