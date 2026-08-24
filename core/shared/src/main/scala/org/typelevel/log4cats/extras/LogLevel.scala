/*
 * Copyright 2018 Typelevel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.typelevel.log4cats.extras

import cats.*

sealed trait LogLevel
object LogLevel {
  case object Error extends LogLevel
  case object Warn extends LogLevel
  case object Info extends LogLevel
  case object Debug extends LogLevel
  case object Trace extends LogLevel

  def fromString(s: String): Option[LogLevel] = s.toLowerCase match {
    case "error" => Some(LogLevel.Error)
    case "warn" => Some(LogLevel.Warn)
    case "info" => Some(LogLevel.Info)
    case "debug" => Some(LogLevel.Debug)
    case "trace" => Some(LogLevel.Trace)
    case "loglevel.error" => Some(LogLevel.Error)
    case "loglevel.warn" => Some(LogLevel.Warn)
    case "loglevel.info" => Some(LogLevel.Info)
    case "loglevel.debug" => Some(LogLevel.Debug)
    case "loglevel.trace" => Some(LogLevel.Trace)
    case _ => None
  }

  implicit val logLevelShow: Show[LogLevel] = Show.show[LogLevel] {
    case Error => "LogLevel.Error"
    case Warn => "LogLevel.Warn"
    case Info => "LogLevel.Info"
    case Debug => "LogLevel.Debug"
    case Trace => "LogLevel.Trace"
  }

  private def toIndex(l: LogLevel): Int = l match {
    case Error => 5
    case Warn => 4
    case Info => 3
    case Debug => 2
    case Trace => 1
  }

  implicit final val logLevelOrder: Order[LogLevel] =
    Order.by[LogLevel, Int](toIndex)

  implicit final val logLevelHash: Hash[LogLevel] = Hash.by(toIndex)
}
