package io.chrisdavenport.log4cats

trait Logger[F[_]]{
  import Logger.{withModifiedString => wMS}

  def isTraceEnabled: F[Boolean]
  def isDebugEnabled: F[Boolean]
  def isInfoEnabled: F[Boolean] 
  def isWarnEnabled: F[Boolean]
  def isErrorEnabled: F[Boolean]

  def error(message: => String): F[Unit]
  def error(t: Throwable)(message: => String): F[Unit]
  def warn(message: => String): F[Unit]
  def warn(t: Throwable)(message: => String): F[Unit]
  def info(message: => String): F[Unit]
  def info(t: Throwable)(message: => String): F[Unit]
  def debug(message: => String): F[Unit]
  def debug(t: Throwable)(message: => String): F[Unit]
  def trace(message: => String): F[Unit]
  def trace(t: Throwable)(message: => String): F[Unit]
  
  def withModifiedString(f: String => String): Logger[F] = wMS[F](this, f)
}

object Logger {
  def apply[F[_]](implicit ev: Logger[F]) = ev

  private def withModifiedString[F[_]](l: Logger[F], f: String => String): Logger[F] = new Logger[F]{
    def isTraceEnabled: F[Boolean] = l.isTraceEnabled
    def isDebugEnabled: F[Boolean] = l.isDebugEnabled
    def isInfoEnabled: F[Boolean] = l.isInfoEnabled
    def isWarnEnabled: F[Boolean] = l.isWarnEnabled
    def isErrorEnabled: F[Boolean] = l.isErrorEnabled
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