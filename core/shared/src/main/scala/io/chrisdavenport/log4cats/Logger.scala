package io.chrisdavenport.log4cats

trait Logger[F[_]] extends MessageLogger[F] with ErrorLogger[F] {
  // Perhaps this should be a typeclass over algebras?
  def withModifiedString(f: String => String): Logger[F] = Logger.withModifiedString[F](this, f)
}

object Logger {
  def apply[F[_]](implicit ev: Logger[F]) = ev

  private def withModifiedString[F[_]](l: Logger[F], f: String => String): Logger[F] =
    new Logger[F] {
      def error(message: => String): F[Unit] = l.error(f(message))
      def error(t: Throwable)(message: => String): F[Unit] = l.error(t)(f(message))
      def warn(message: => String): F[Unit] = l.warn(f(message))
      def warn(t: Throwable)(message: => String): F[Unit] = l.warn(t)(f(message))
      def info(message: => String): F[Unit] = l.info(f(message))
      def info(t: Throwable)(message: => String): F[Unit] = l.info(t)(f(message))
      def debug(message: => String): F[Unit] = l.debug(f(message))
      def debug(t: Throwable)(message: => String): F[Unit] = l.debug(t)(f(message))
      def trace(message: => String): F[Unit] = l.trace(f(message))
      def trace(t: Throwable)(message: => String): F[Unit] = l.trace(t)(f(message))
    }
}
