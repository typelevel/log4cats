package io.chrisdavenport.log4cats.slf4j

import cats.implicits._
import cats.effect.Sync
import io.chrisdavenport.log4cats.Logger
import org.slf4j.{Logger => Base, LoggerFactory}
import language.experimental.macros

object Slf4jLogger {

  def fromName[F[_]: Sync](name: String): Logger[F] = 
    fromLogger[F](LoggerFactory.getLogger(name))

  def fromClass[F[_]: Sync](clazz: Class[_]): Logger[F] =
    fromLogger[F](LoggerFactory.getLogger(clazz))

  def getLogger[F[_]: Sync]: Logger[F] = macro Slf4jMacros.getLoggerImpl[F[_]]

  def fromLogger[F[_]: Sync](logger: Base): Logger[F] = new Logger[F]{

    def isDebugEnabled: F[Boolean] = Sync[F].delay(logger.isDebugEnabled)
    def isErrorEnabled: F[Boolean] = Sync[F].delay(logger.isErrorEnabled)
    def isInfoEnabled: F[Boolean] = Sync[F].delay(logger.isInfoEnabled)
    def isTraceEnabled: F[Boolean] = Sync[F].delay(logger.isTraceEnabled)
    def isWarnEnabled: F[Boolean] = Sync[F].delay(logger.isWarnEnabled)

    def debug(t: Throwable)(message: => String): F[Unit] = isDebugEnabled.ifM(
      Sync[F].delay(logger.debug(message, t)),
      Sync[F].unit
    )
    def debug(message: => String): F[Unit] = isDebugEnabled.ifM(
      Sync[F].delay(logger.debug(message)),
      Sync[F].unit
    )
    def error(t: Throwable)(message: => String): F[Unit] = isErrorEnabled.ifM(
      Sync[F].delay(logger.error(message, t)),
      Sync[F].unit
    )
    def error(message: => String): F[Unit] = isErrorEnabled.ifM(
      Sync[F].delay(logger.error(message)),
      Sync[F].unit
    )
    def info(t: Throwable)(message: => String): F[Unit] = isInfoEnabled.ifM(
      Sync[F].delay(logger.info(message, t)),
      Sync[F].unit
    )
    def info(message: => String): F[Unit] = isInfoEnabled.ifM(
      Sync[F].delay(logger.info(message)),
      Sync[F].unit
    )
    
    def trace(t: Throwable)(message: => String): F[Unit] = isTraceEnabled.ifM(
      Sync[F].delay(logger.trace(message, t)),
      Sync[F].unit
    )
    def trace(message: => String): F[Unit] = isTraceEnabled.ifM(
      Sync[F].delay(logger.trace(message)),
      Sync[F].unit
    )
    def warn(t: Throwable)(message: => String): F[Unit] = isWarnEnabled.ifM(
      Sync[F].delay(logger.warn(message, t)),
      Sync[F].unit
    )
    def warn(message: => String): F[Unit] = isWarnEnabled.ifM(
      Sync[F].delay(logger.warn(message)),
      Sync[F].unit
    )
  }
}