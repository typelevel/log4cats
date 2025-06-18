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

sealed trait DefferedLogLevel
object DefferedLogLevel {
  case object Error extends DefferedLogLevel
  case object Warn extends DefferedLogLevel
  case object Info extends DefferedLogLevel
  case object Debug extends DefferedLogLevel
  case object Trace extends DefferedLogLevel

  def fromString(s: String): Option[DefferedLogLevel] = s.toLowerCase match {
    case "error" => Some(DefferedLogLevel.Error)
    case "warn" => Some(DefferedLogLevel.Warn)
    case "info" => Some(DefferedLogLevel.Info)
    case "debug" => Some(DefferedLogLevel.Debug)
    case "trace" => Some(DefferedLogLevel.Trace)
    case "loglevel.error" => Some(DefferedLogLevel.Error)
    case "loglevel.warn" => Some(DefferedLogLevel.Warn)
    case "loglevel.info" => Some(DefferedLogLevel.Info)
    case "loglevel.debug" => Some(DefferedLogLevel.Debug)
    case "loglevel.trace" => Some(DefferedLogLevel.Trace)
    case _ => None
  }

  implicit val logLevelShow: Show[DefferedLogLevel] = Show.show[DefferedLogLevel] {
    case Error => "DefferedLogLevel.Error"
    case Warn => "DefferedLogLevel.Warn"
    case Info => "DefferedLogLevel.Info"
    case Debug => "DefferedLogLevel.Debug"
    case Trace => "DefferedLogLevel.Trace"
  }

  private def toIndex(l: DefferedLogLevel): Int = l match {
    case Error => 5
    case Warn => 4
    case Info => 3
    case Debug => 2
    case Trace => 1
  }

  implicit final val logLevelOrder: Order[DefferedLogLevel] =
    Order.by[DefferedLogLevel, Int](toIndex)

  implicit final val logLevelHash: Hash[DefferedLogLevel] = Hash.by(toIndex)
}
