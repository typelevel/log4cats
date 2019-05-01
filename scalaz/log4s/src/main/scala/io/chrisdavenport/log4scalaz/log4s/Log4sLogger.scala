package io.chrisdavenport.log4cats.log4s

import io.chrisdavenport.log4cats._
import org.log4s.{Logger => Base}
import scalaz.zio.{UIO, ZIO}

object Log4sLogger {

  def createLocal = fromLog4s(org.log4s.getLogger)
  def createByName(name: String) = fromLog4s(org.log4s.getLogger(name))
  def createByClass(clazz: Class[_]) = fromLog4s(org.log4s.getLogger(clazz))

  def fromLog4s(logger: Base): SelfAwareLogger[UIO] =
    new SelfAwareLogger[UIO] {
      override def isTraceEnabled: UIO[Boolean] =
        ZIO.succeed(logger.isTraceEnabled)
      override def isDebugEnabled: UIO[Boolean] =
        ZIO.succeed(logger.isDebugEnabled)
      override def isInfoEnabled: UIO[Boolean] =
        ZIO.succeed(logger.isInfoEnabled)
      override def isWarnEnabled: UIO[Boolean] =
        ZIO.succeed(logger.isWarnEnabled)
      override def isErrorEnabled: UIO[Boolean] =
        ZIO.succeed(logger.isErrorEnabled)

      override def error(message: => String): UIO[Unit] =
        ZIO.succeed(logger.error(message))
      override def error(t: Throwable)(message: => String): UIO[Unit] =
        ZIO.succeed(logger.error(t)(message))

      override def warn(message: => String): UIO[Unit] =
        ZIO.succeed(logger.warn(message))
      override def warn(t: Throwable)(message: => String): UIO[Unit] =
        ZIO.succeed(logger.warn(t)(message))

      override def info(message: => String): UIO[Unit] =
        ZIO.succeed(logger.info(message))
      override def info(t: Throwable)(message: => String): UIO[Unit] =
        ZIO.succeed(logger.info(t)(message))

      override def debug(message: => String): UIO[Unit] =
        ZIO.succeed(logger.debug(message))
      override def debug(t: Throwable)(message: => String): UIO[Unit] =
        ZIO.succeed(logger.debug(t)(message))

      override def trace(message: => String): UIO[Unit] =
        ZIO.succeed(logger.trace(message))
      override def trace(t: Throwable)(message: => String): UIO[Unit] =
        ZIO.succeed(logger.trace(t)(message))
    }
}
