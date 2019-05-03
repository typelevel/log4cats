package io.chrisdavenport.log4cats

import cats._

trait MessageLogger[F[_]] {
  def error(message: => String): F[Unit]
  def warn(message: => String): F[Unit]
  def info(message: => String): F[Unit]
  def debug(message: => String): F[Unit]
  def trace(message: => String): F[Unit]
  def mapK[G[_]](fk: F ~> G): MessageLogger[G] =
    MessageLogger.mapK(fk)(this)
}

object MessageLogger {
  def apply[F[_]](implicit ev: MessageLogger[F]): MessageLogger[F] = ev

  private def mapK[G[_], F[_]](f: G ~> F)(logger: MessageLogger[G]): MessageLogger[F] =
    new MessageLogger[F] {
      def error(message: => String): F[Unit] =
        f(logger.error(message))
      def warn(message: => String): F[Unit] =
        f(logger.warn(message))
      def info(message: => String): F[Unit] =
        f(logger.info(message))
      def debug(message: => String): F[Unit] =
        f(logger.debug(message))
      def trace(message: => String): F[Unit] =
        f(logger.trace(message))
    }
}
