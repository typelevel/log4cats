package org.typelevel.log4cats

import cats.effect.Sync
import org.slf4j.{Logger => JLogger}

package object slf4j {
  implicit def loggerFactoryforSync[F[_]: Sync]: Slf4jFactory[F] = new Slf4jFactory[F] {
    override def getLoggerFromName(name: String): SelfAwareStructuredLogger[F] =
      Slf4jLogger.getLoggerFromName(name)

    override def getLoggerFromSlf4j(logger: JLogger): SelfAwareStructuredLogger[F] =
      Slf4jLogger.getLoggerFromSlf4j(logger)

    override def fromName(name: String): F[SelfAwareStructuredLogger[F]] =
      Slf4jLogger.fromName(name)

    override def fromSlf4j(logger: JLogger): F[SelfAwareStructuredLogger[F]] =
      Slf4jLogger.fromSlf4j(logger)
  }
}
