package io.chrisdavenport.log4cats.log4s

import io.chrisdavenport.log4cats._
import org.log4s.{Logger => Base}
import scalaz.zio.UIO

object Log4sLogger {

  def createLocal: SelfAwareLogger[UIO] = fromLog4s(org.log4s.getLogger)
  def createByName(name: String): SelfAwareLogger[UIO] = fromLog4s(org.log4s.getLogger(name))
  def createByClass(clazz: Class[_]): SelfAwareLogger[UIO] = fromLog4s(org.log4s.getLogger(clazz))

  def fromLog4s(logger: Base): SelfAwareLogger[UIO] = new SelfAwareLogger[UIO] {
    override def isTraceEnabled: UIO[Boolean] = UIO.apply(logger.isTraceEnabled)
    override def isDebugEnabled: UIO[Boolean] = UIO.apply(logger.isDebugEnabled)
    override def isInfoEnabled: UIO[Boolean] = UIO.apply(logger.isInfoEnabled)
    override def isWarnEnabled: UIO[Boolean] = UIO.apply(logger.isWarnEnabled)
    override def isErrorEnabled: UIO[Boolean] = UIO.apply(logger.isErrorEnabled)

    override def error(message: => String): UIO[Unit] = UIO.apply(logger.error(message))
    override def error(t: Throwable)(message: => String): UIO[Unit] =
      UIO.apply(logger.error(t)(message))

    override def warn(message: => String): UIO[Unit] = UIO.apply(logger.warn(message))
    override def warn(t: Throwable)(message: => String): UIO[Unit] =
      UIO.apply(logger.warn(t)(message))

    override def info(message: => String): UIO[Unit] = UIO.apply(logger.info(message))
    override def info(t: Throwable)(message: => String): UIO[Unit] =
      UIO.apply(logger.info(t)(message))

    override def debug(message: => String): UIO[Unit] = UIO.apply(logger.debug(message))
    override def debug(t: Throwable)(message: => String): UIO[Unit] =
      UIO.apply(logger.debug(t)(message))

    override def trace(message: => String): UIO[Unit] = UIO.apply(logger.trace(message))
    override def trace(t: Throwable)(message: => String): UIO[Unit] =
      UIO.apply(logger.trace(t)(message))
  }
}
