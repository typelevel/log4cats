package org.typelevel.log4cats.slf4j

import cats.effect.Sync
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.internal.Slf4jLoggerInternal
import org.slf4j.{Logger => JLogger}

object Slf4jLogger extends Slf4jLoggerPlatform {

  def getLogger[F[_]: Sync](implicit name: LoggerName): SelfAwareStructuredLogger[F] =
    getLoggerFromName(name.value)

  def getLoggerFromName[F[_]: Sync](name: String): SelfAwareStructuredLogger[F] =
    getLoggerFromSlf4j(org.slf4j.LoggerFactory.getLogger(name))

  def getLoggerFromClass[F[_]: Sync](clazz: Class[_]): SelfAwareStructuredLogger[F] =
    getLoggerFromSlf4j[F](org.slf4j.LoggerFactory.getLogger(clazz))

  def getLoggerFromSlf4j[F[_]: Sync](logger: JLogger): SelfAwareStructuredLogger[F] =
    new Slf4jLoggerInternal.Slf4jLogger(logger)

  def create[F[_]: Sync](implicit name: LoggerName): F[SelfAwareStructuredLogger[F]] =
    Sync[F].delay(getLoggerFromName(name.value))

  def fromName[F[_]: Sync](name: String): F[SelfAwareStructuredLogger[F]] =
    Sync[F].delay(getLoggerFromName(name))

  def fromClass[F[_]: Sync](clazz: Class[_]): F[SelfAwareStructuredLogger[F]] =
    Sync[F].delay(getLoggerFromClass(clazz))

  def fromSlf4j[F[_]: Sync](logger: JLogger): F[SelfAwareStructuredLogger[F]] =
    Sync[F].delay(getLoggerFromSlf4j[F](logger))

}
