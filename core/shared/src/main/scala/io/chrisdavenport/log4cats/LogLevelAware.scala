package io.chrisdavenport.log4cats

trait LogLevelAware[F[_]]{
  def isTraceEnabled: F[Boolean]
  def isDebugEnabled: F[Boolean]
  def isInfoEnabled: F[Boolean] 
  def isWarnEnabled: F[Boolean]
  def isErrorEnabled: F[Boolean]
}