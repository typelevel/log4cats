package org.typelevel.log4cats

import scala.annotation.implicitNotFound

@implicitNotFound("""|
                     |Implicit not found for LoggerFactory[${F}].
                     |
                     |Information can be found here: https://log4cats.github.io/logging-capability.html
                     |""")
trait LoggerFactory[F[_]] extends LoggerFactoryGen[F, SelfAwareStructuredLogger[F]]
object LoggerFactory extends LoggerFactoryCompanion

private[log4cats] trait LoggerFactoryCompanion {
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
}
