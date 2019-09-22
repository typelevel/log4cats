package io.chrisdavenport.log4cats.noop

import cats.Applicative
import io.chrisdavenport.log4cats.{LoggerFactory, SelfAwareStructuredLogger}

object NoOpLoggerFactory {
  def apply[F[_]: Applicative]: LoggerFactory[F] = new LoggerFactory[F] {

    override def fromName(name: String): F[SelfAwareStructuredLogger[F]] =
      Applicative[F].pure(NoOpLogger.impl)

    override def fromClass(clazz: Class[_]): F[SelfAwareStructuredLogger[F]] =
      Applicative[F].pure(NoOpLogger.impl)
  }
}
