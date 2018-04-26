package io.chrisdavenport.log4cats

import cats.effect.Sync
import org.log4s.{Logger => Base}

trait Logger[F[_]]{

  def isTraceEnabled: F[Boolean]
  def isDebugEnabled: F[Boolean]
  def isInfoEnabled: F[Boolean] 
  def isWarnEnabled: F[Boolean]
  def isErrorEnabled: F[Boolean]

  def error(message: => String): F[Unit]
  def error(t: Throwable)(message: => String): F[Unit]
  def warn(message: => String): F[Unit]
  def warn(t: Throwable)(message: => String): F[Unit]
  def info(message: => String): F[Unit]
  def info(t: Throwable)(message: => String): F[Unit]
  def debug(message: => String): F[Unit]
  def debug(t: Throwable)(message: => String): F[Unit]
  def trace(message: => String): F[Unit]
  def trace(t: Throwable)(message: => String): F[Unit]
}

object Logger {
  def apply[F[_]](implicit ev: Logger[F]) = ev


  def createLocal[F[_]: Sync] = fromLog4s[F](org.log4s.getLogger)
  def createByName[F[_]: Sync](name: String) = fromLog4s[F](org.log4s.getLogger(name))
  def createByClass[F[_]: Sync](clazz: Class[_]) = fromLog4s[F](org.log4s.getLogger(clazz))


  def fromLog4s[F[_]: Sync](logger: Base): Logger[F] = new Logger[F]{

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