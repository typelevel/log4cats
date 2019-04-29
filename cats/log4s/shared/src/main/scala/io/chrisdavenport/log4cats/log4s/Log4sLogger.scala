package io.chrisdavenport.log4cats.log4s

import io.chrisdavenport.log4cats._
import cats.effect.Sync
import org.log4s.{Logger => Base}

object Log4sLogger {

  def createLocal[F[_]: Sync] = fromLog4s[F](org.log4s.getLogger)
  def createByName[F[_]: Sync](name: String) = fromLog4s[F](org.log4s.getLogger(name))
  def createByClass[F[_]: Sync](clazz: Class[_]) = fromLog4s[F](org.log4s.getLogger(clazz))

  def fromLog4s[F[_]: Sync](logger: Base): SelfAwareLogger[F] = new SelfAwareLogger[F] {

    override def isTraceEnabled: F[Boolean] =
      Sync[F].delay(logger.isTraceEnabled)
    override def isDebugEnabled: F[Boolean] =
      Sync[F].delay(logger.isDebugEnabled)
    override def isInfoEnabled: F[Boolean] =
      Sync[F].delay(logger.isInfoEnabled)
    override def isWarnEnabled: F[Boolean] =
      Sync[F].delay(logger.isWarnEnabled)
    override def isErrorEnabled: F[Boolean] =
      Sync[F].delay(logger.isErrorEnabled)

    override def error(message: => String): F[Unit] =
      Sync[F].delay(logger.error(message))
    override def error(t: Throwable)(message: => String): F[Unit] =
      Sync[F].delay(logger.error(t)(message))

    override def warn(message: => String): F[Unit] =
      Sync[F].delay(logger.warn(message))
    override def warn(t: Throwable)(message: => String): F[Unit] =
      Sync[F].delay(logger.warn(t)(message))

    override def info(message: => String): F[Unit] =
      Sync[F].delay(logger.info(message))
    override def info(t: Throwable)(message: => String): F[Unit] =
      Sync[F].delay(logger.info(t)(message))

    override def debug(message: => String): F[Unit] =
      Sync[F].delay(logger.debug(message))
    override def debug(t: Throwable)(message: => String): F[Unit] =
      Sync[F].delay(logger.debug(t)(message))

    override def trace(message: => String): F[Unit] =
      Sync[F].delay(logger.trace(message))
    override def trace(t: Throwable)(message: => String): F[Unit] =
      Sync[F].delay(logger.trace(t)(message))
  }
}
