package io.chrisdavenport.log4cats.slf4j

import cats.effect.Sync
import io.chrisdavenport.log4cats.{LoggerFactory, SelfAwareStructuredLogger}

object Slf4jLoggerFactory {

  def apply[F[_]: Sync]: LoggerFactory[F] = new Slf4jLoggerFactory[F]

  private class Slf4jLoggerFactory[F[_]: Sync] extends LoggerFactory[F] {

    override def fromName(name: String): F[SelfAwareStructuredLogger[F]] =
      Slf4jLogger.fromName(name)

    override def fromClass(clazz: Class[_]): F[SelfAwareStructuredLogger[F]] =
      Slf4jLogger.fromClass(clazz)
  }
}
