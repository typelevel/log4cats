package io.chrisdavenport.log4cats

trait ErrorLogger[F[_]] {
  def error(t: Throwable)(message: => String): F[Unit]
  def warn(t: Throwable)(message: => String): F[Unit]
  def info(t: Throwable)(message: => String): F[Unit]
  def debug(t: Throwable)(message: => String): F[Unit]
  def trace(t: Throwable)(message: => String): F[Unit]
}

object ErrorLogger {
  def apply[F[_]](implicit ev: ErrorLogger[F]): ErrorLogger[F] = ev
}
