package io.chrisdavenport.log4cats.extras

final case class LogMessage(level: LogLevel, t: Option[Throwable], message: String)