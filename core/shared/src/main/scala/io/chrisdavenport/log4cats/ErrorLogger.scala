package io.chrisdavenport.log4cats

import cats._
trait ErrorLogger[F[_]] {
  def error(t: Throwable)(message: => String): F[Unit]
  def warn(t: Throwable)(message: => String): F[Unit]
  def info(t: Throwable)(message: => String): F[Unit]
  def debug(t: Throwable)(message: => String): F[Unit]
  def trace(t: Throwable)(message: => String): F[Unit]
  def mapK[G[_]](fk: F ~> G): ErrorLogger[G] = 
    ErrorLogger.mapK(fk)(this)
}

object ErrorLogger {
  def apply[F[_]](implicit ev: ErrorLogger[F]): ErrorLogger[F] = ev


  private def mapK[G[_], F[_]](f: G ~> F)(logger: ErrorLogger[G]): ErrorLogger[F] =
    new ErrorLogger[F] {
      def error(t: Throwable)(message: => String): F[Unit] =
        f(logger.error(t)(message))
      def warn(t: Throwable)(message: => String): F[Unit] =
        f(logger.warn(t)(message))
      def info(t: Throwable)(message: => String): F[Unit] =
        f(logger.info(t)(message))
      def debug(t: Throwable)(message: => String): F[Unit] =
        f(logger.debug(t)(message))
      def trace(t: Throwable)(message: => String): F[Unit] =
        f(logger.trace(t)(message))
    }

}
