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

  def withContext[F[_]](sl: StructuredLogger[F])(ctx: (String, String)*): MessageLogger[F] =
    new MessageLogger[F]{
      def error(message: => String): F[Unit] = sl.error(ctx:_*)(message)
      def warn(message: => String): F[Unit] = sl.warn(ctx:_*)(message)
      def info(message: => String): F[Unit] = sl.info(ctx:_*)(message)
      def debug(message: => String): F[Unit] = sl.debug(ctx:_*)(message)
      def trace(message: => String): F[Unit] = sl.trace(ctx:_*)(message)
    }
}