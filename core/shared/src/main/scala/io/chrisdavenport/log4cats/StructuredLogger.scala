package io.chrisdavenport.log4cats

trait StructuredLogger[F[_]] extends Logger[F] {
  def trace(ctx: (String, String)*)(msg: => String): F[Unit]
  def debug(ctx: (String, String)*)(msg: => String): F[Unit]
  def info(ctx: (String, String)*)(msg: => String): F[Unit]
  def warn(ctx: (String, String)*)(msg: => String): F[Unit]
  def error(ctx: (String, String)*)(msg: => String): F[Unit]
}

object StructuredLogger{
  def apply[F[_]](implicit ev: StructuredLogger[F]): StructuredLogger[F] = ev
}