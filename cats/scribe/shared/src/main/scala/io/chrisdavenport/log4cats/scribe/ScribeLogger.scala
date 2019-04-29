package io.chrisdavenport.log4cats.scribe

import io.chrisdavenport.log4cats.Logger
import _root_.scribe.{Logger => Base}
import cats.effect.Sync

object ScribeLogger {

  def empty[F[_]: Sync] = fromLogger[F](Base.empty)
  def root[F[_]: Sync] = fromLogger[F](Base.root)
  def byName[F[_]: Sync](name: String) = fromLogger[F](Base(name))

  def fromLogger[F[_]: Sync](logger: Base): Logger[F] = new Logger[F] {

    def error(message: => String): F[Unit] =
      Sync[F].delay(logger.error(message))
    def error(t: Throwable)(message: => String): F[Unit] =
      Sync[F].delay(logger.error(message, t))
    def warn(message: => String): F[Unit] =
      Sync[F].delay(logger.warn(message))
    def warn(t: Throwable)(message: => String): F[Unit] =
      Sync[F].delay(logger.warn(message, t))
    def info(message: => String): F[Unit] =
      Sync[F].delay(logger.info(message))
    def info(t: Throwable)(message: => String): F[Unit] =
      Sync[F].delay(logger.info(message, t))
    def debug(message: => String): F[Unit] =
      Sync[F].delay(logger.debug(message))
    def debug(t: Throwable)(message: => String): F[Unit] =
      Sync[F].delay(logger.debug(message, t))
    def trace(message: => String): F[Unit] =
      Sync[F].delay(logger.trace(message))
    def trace(t: Throwable)(message: => String): F[Unit] =
      Sync[F].delay(logger.trace(message, t))
  }

}
