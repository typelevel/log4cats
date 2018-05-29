package io.chrisdavenport.log4cats

trait MDCLogger[F[_]]{
  def trace(ctx: (String, String)*)(msg: => String): F[Unit]
  def debug(ctx: (String, String)*)(msg: => String): F[Unit]
  def info(ctx: (String, String)*)(msg: => String): F[Unit]
  def warn(ctx: (String, String)*)(msg: => String): F[Unit]
  def error(ctx: (String, String)*)(msg: => String): F[Unit]
}