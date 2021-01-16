/*
 * Copyright 2020 Christopher Davenport
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

import cats._

sealed trait LogLevel
object LogLevel {
  case object Error extends LogLevel
  case object Warn extends LogLevel
  case object Info extends LogLevel
  case object Debug extends LogLevel
  case object Trace extends LogLevel

  implicit val logLevelShow: Show[LogLevel] = Show.show[LogLevel] {
    case Error => "LogLevel.Error"
    case Warn => "LogLevel.Warn"
    case Info => "LogLevel.Info"
    case Debug => "LogLevel.Debug"
    case Trace => "LogLevel.Trace"
  }

  implicit final val logLevelOrder: Order[LogLevel] =
    Order.by[LogLevel, Int] {
      case Error => 5
      case Warn => 4
      case Info => 3
      case Debug => 2
      case Trace => 1
    }
}
