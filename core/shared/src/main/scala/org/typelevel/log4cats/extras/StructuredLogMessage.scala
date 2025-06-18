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

import cats.Show
import cats.syntax.all.*
import org.typelevel.log4cats.StructuredLogger

final case class StructuredLogMessage(
    level: DefferedLogLevel,
    context: Map[String, String],
    throwableOpt: Option[Throwable],
    message: String
)
object StructuredLogMessage {
  def log[F[_]](sm: StructuredLogMessage, l: StructuredLogger[F]): F[Unit] = sm match {
    case StructuredLogMessage(DefferedLogLevel.Trace, ctx, Some(t), m) => l.trace(ctx, t)(m)
    case StructuredLogMessage(DefferedLogLevel.Trace, ctx, None, m) => l.trace(ctx)(m)

    case StructuredLogMessage(DefferedLogLevel.Debug, ctx, Some(t), m) => l.debug(ctx, t)(m)
    case StructuredLogMessage(DefferedLogLevel.Debug, ctx, None, m) => l.debug(ctx)(m)

    case StructuredLogMessage(DefferedLogLevel.Info, ctx, Some(t), m) => l.info(ctx, t)(m)
    case StructuredLogMessage(DefferedLogLevel.Info, ctx, None, m) => l.info(ctx)(m)

    case StructuredLogMessage(DefferedLogLevel.Warn, ctx, Some(t), m) => l.warn(ctx, t)(m)
    case StructuredLogMessage(DefferedLogLevel.Warn, ctx, None, m) => l.warn(ctx)(m)

    case StructuredLogMessage(DefferedLogLevel.Error, ctx, Some(t), m) => l.error(ctx, t)(m)
    case StructuredLogMessage(DefferedLogLevel.Error, ctx, None, m) => l.error(ctx)(m)
  }

  implicit val structuredLogMessageShow: Show[StructuredLogMessage] =
    Show.show[StructuredLogMessage] { l =>
      show"StructuredLogMessage(${l.level},${l.context},${l.throwableOpt.map(_.getMessage)},${l.message})"
    }
}
