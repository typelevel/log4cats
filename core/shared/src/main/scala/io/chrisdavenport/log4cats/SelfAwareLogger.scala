package io.chrisdavenport.log4cats

trait SelfAwareLogger[F[_]] extends Logger[F] {
  def isTraceEnabled: F[Boolean]
  def isDebugEnabled: F[Boolean]
  def isInfoEnabled: F[Boolean] 
  def isWarnEnabled: F[Boolean]
  def isErrorEnabled: F[Boolean]
}
object SelfAwareLogger {
  def apply[F[_]](implicit ev: SelfAwareLogger[F]): SelfAwareLogger[F] = ev
}