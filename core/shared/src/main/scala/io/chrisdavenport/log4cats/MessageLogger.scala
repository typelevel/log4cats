package io.chrisdavenport.log4cats

trait MessageLogger[F[_]] {
  def error(message: => String): F[Unit]
  def warn(message: => String): F[Unit]
  def info(message: => String): F[Unit]
  def debug(message: => String): F[Unit]
  def trace(message: => String): F[Unit]
}

object MessageLogger {
  def apply[F[_]](implicit ev: MessageLogger[F]): MessageLogger[F] = ev
}
