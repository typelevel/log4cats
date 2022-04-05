package org.typelevel.log4cats

import org.typelevel.log4cats.internal.LoggerNameMacro

trait LoggerNameCompat {
  implicit def name: LoggerName = macro LoggerNameMacro.getLoggerName
}
