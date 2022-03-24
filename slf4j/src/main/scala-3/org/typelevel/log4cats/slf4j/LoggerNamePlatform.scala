package org.typelevel.log4cats.slf4j

import org.typelevel.log4cats.slf4j.internal.GetLoggerMacros

import scala.quoted.*

trait LoggerNamePlatform {
  implicit inline def name: LoggerName =
    ${ GetLoggerMacros.getLoggerName }
}
