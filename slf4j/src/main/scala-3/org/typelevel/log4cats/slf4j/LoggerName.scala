package org.typelevel.log4cats.slf4j

import org.typelevel.log4cats.slf4j.internal.GetLoggerMacros

import scala.quoted.*

object LoggerName {
  implicit inline def name: LoggerName =
    ${GetLoggerMacros.getLoggerName }
}

final case class LoggerName(value: String)
