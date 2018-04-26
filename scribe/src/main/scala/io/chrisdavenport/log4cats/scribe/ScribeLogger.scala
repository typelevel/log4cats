package io.chrisdavenport.log4cats.scribe

import io.chrisdavenport.log4cats.Logger
import scribe.{Logger => Base}
import cats.effect.Sync

object ScribeLogger {

  def fromLogger[F[_]: Sync](logger: Base): Logger[F] = new Logger[F]{
    // Law Abiding But Not What We Want
    def isTraceEnabled: F[Boolean] = 
      Sync[F].delay(???)
    def isDebugEnabled: F[Boolean] = 
      Sync[F].delay(???)
    def isInfoEnabled: F[Boolean] =
      Sync[F].delay(???)
    def isWarnEnabled: F[Boolean] = 
      Sync[F].delay(???)
    def isErrorEnabled: F[Boolean] = 
      Sync[F].delay(???)

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