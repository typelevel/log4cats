package org.typelevel.log4cats

trait LoggerFactoryGen[F[_], LoggerType] {
  def getLogger(implicit name: LoggerName): LoggerType = getLoggerFromName(name.value)
  def getLoggerFromClass(clazz: Class[_]): LoggerType = getLoggerFromName(clazz.getName)
  def create(implicit name: LoggerName): F[LoggerType] = fromName(name.value)
  def fromClass(clazz: Class[_]): F[LoggerType] = fromName(clazz.getName)
  def getLoggerFromName(name: String): LoggerType
  def fromName(name: String): F[LoggerType]
}
