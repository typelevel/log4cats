package org.typelevel.log4cats.slf4j

import internal.GetLoggerMacros

object LoggerName {
  implicit def name: LoggerName = macro GetLoggerMacros.getLoggerName
}

final case class LoggerName(value: String)