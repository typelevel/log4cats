package org.typelevel.log4cats.slf4j

import org.typelevel.log4cats.slf4j.internal.GetLoggerMacros

trait LoggerNamePlatform {
  implicit def name: LoggerName = macro GetLoggerMacros.getLoggerName
}
