package org.typelevel.log4cats
package slf4j

import cats.effect.Sync
import org.slf4j.{Logger => JLogger}

trait LoggerFactory[F[_]] {
  def getLogger(implicit name: LoggerName): SelfAwareStructuredLogger[F]
  def getLoggerFromName(name: String): SelfAwareStructuredLogger[F]
  def getLoggerFromClass(clazz: Class[_]): SelfAwareStructuredLogger[F]
  def getLoggerFromSlf4j(logger: JLogger): SelfAwareStructuredLogger[F]
  def create(implicit name: LoggerName): F[SelfAwareStructuredLogger[F]]
  def fromName(name: String): F[SelfAwareStructuredLogger[F]]
  def fromClass(clazz: Class[_]): F[SelfAwareStructuredLogger[F]]
  def fromSlf4j(logger: JLogger): F[SelfAwareStructuredLogger[F]]
}

object LoggerFactory {
  def apply[F[_]: LoggerFactory]: LoggerFactory[F] = implicitly

  implicit def forSync[F[_]: Sync]: LoggerFactory[F] = new LoggerFactory[F] {
    override def getLogger(implicit name: LoggerName): SelfAwareStructuredLogger[F] =
      Slf4jLogger.getLogger

    override def getLoggerFromName(name: String): SelfAwareStructuredLogger[F] =
      Slf4jLogger.getLoggerFromName(name)
    override def getLoggerFromClass(clazz: Class[_]): SelfAwareStructuredLogger[F] =
      Slf4jLogger.getLoggerFromClass(clazz)
    override def getLoggerFromSlf4j(logger: JLogger): SelfAwareStructuredLogger[F] =
      Slf4jLogger.getLoggerFromSlf4j(logger)

    def create(implicit name: LoggerName): F[SelfAwareStructuredLogger[F]] =
      Slf4jLogger.create

    override def fromName(name: String): F[SelfAwareStructuredLogger[F]] =
      Slf4jLogger.fromName(name)

    override def fromClass(clazz: Class[_]): F[SelfAwareStructuredLogger[F]] =
      Slf4jLogger.fromClass(clazz)

    override def fromSlf4j(logger: JLogger): F[SelfAwareStructuredLogger[F]] =
      Slf4jLogger.fromSlf4j(logger)
  }

  def getLogger[F[_]](implicit
      lf: LoggerFactory[F],
      name: LoggerName
  ): SelfAwareStructuredLogger[F] =
    lf.getLogger
  def getLoggerFromName[F[_]](name: String)(implicit
      lf: LoggerFactory[F]
  ): SelfAwareStructuredLogger[F] =
    lf.getLoggerFromName(name)

  def getLoggerFromClass[F[_]](clazz: Class[_])(implicit
      lf: LoggerFactory[F]
  ): SelfAwareStructuredLogger[F] =
    lf.getLoggerFromClass(clazz)

  def getLoggerFromSlf4j[F[_]](logger: JLogger)(implicit
      lf: LoggerFactory[F]
  ): SelfAwareStructuredLogger[F] =
    lf.getLoggerFromSlf4j(logger)
  def create[F[_]](implicit
      lf: LoggerFactory[F],
      name: LoggerName
  ): F[SelfAwareStructuredLogger[F]] =
    lf.create
  def fromName[F[_]](name: String)(implicit lf: LoggerFactory[F]): F[SelfAwareStructuredLogger[F]] =
    lf.fromName(name)
  def fromClass[F[_]](clazz: Class[_])(implicit
      lf: LoggerFactory[F]
  ): F[SelfAwareStructuredLogger[F]] =
    lf.fromClass(clazz)
  def fromSlf4j[F[_]](logger: JLogger)(implicit
      lf: LoggerFactory[F]
  ): F[SelfAwareStructuredLogger[F]] =
    lf.fromSlf4j(logger)

}
