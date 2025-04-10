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
import cats.syntax.all.*
import org.typelevel.log4cats.Logger

final case class LogMessage(level: LogLevel, t: Option[Throwable], message: String)
object LogMessage {
  implicit val logMessageShow: Show[LogMessage] =
    Show.show[LogMessage](l => show"LogMessage(${l.level},${l.t.map(_.getMessage)},${l.message})")

  def log[F[_]](sm: LogMessage, l: Logger[F]): F[Unit] = sm match {
    case LogMessage(LogLevel.Trace, Some(t), m) => l.trace(t)(m)
    case LogMessage(LogLevel.Trace, None, m) => l.trace(m)

    case LogMessage(LogLevel.Debug, Some(t), m) => l.debug(t)(m)
    case LogMessage(LogLevel.Debug, None, m) => l.debug(m)

    case LogMessage(LogLevel.Info, Some(t), m) => l.info(t)(m)
    case LogMessage(LogLevel.Info, None, m) => l.info(m)

    case LogMessage(LogLevel.Warn, Some(t), m) => l.warn(t)(m)
    case LogMessage(LogLevel.Warn, None, m) => l.warn(m)

    case LogMessage(LogLevel.Error, Some(t), m) => l.error(t)(m)
    case LogMessage(LogLevel.Error, None, m) => l.error(m)
  }
}
