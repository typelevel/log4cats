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
import cats.data.*
import cats.syntax.all.*
import org.typelevel.log4cats.*

/**
 * A `SelfAwareLogger` implemented using `cats.data.WriterT`.
 *
 * >>> WARNING: READ BEFORE USAGE! <<<
 * https://github.com/typelevel/log4cats/blob/main/core/shared/src/main/scala/org/typelevel/log4cats/extras/README.md
 * >>> WARNING: READ BEFORE USAGE! <<<
 *
 * If a `SelfAwareLogger` is needed for test code, the `testing` module provides a better option:
 * `org.typelevel.log4cats.testing.TestingLogger`
 */
object WriterTLogger {
  def apply[F[_]: Applicative, G[_]: Alternative](
      traceEnabled: Boolean = true,
      debugEnabled: Boolean = true,
      infoEnabled: Boolean = true,
      warnEnabled: Boolean = true,
      errorEnabled: Boolean = true
  ): SelfAwareLogger[WriterT[F, G[LogMessage], *]] =
    new SelfAwareLogger[WriterT[F, G[LogMessage], *]] {
      override def isTraceEnabled: WriterT[F, G[LogMessage], Boolean] = isEnabled(traceEnabled)
      override def isDebugEnabled: WriterT[F, G[LogMessage], Boolean] = isEnabled(debugEnabled)
      override def isInfoEnabled: WriterT[F, G[LogMessage], Boolean] = isEnabled(infoEnabled)
      override def isWarnEnabled: WriterT[F, G[LogMessage], Boolean] = isEnabled(warnEnabled)
      override def isErrorEnabled: WriterT[F, G[LogMessage], Boolean] = isEnabled(errorEnabled)

      override def trace(t: Throwable)(message: => String): WriterT[F, G[LogMessage], Unit] =
        build(traceEnabled, LogLevel.Trace, t.some, message)
      override def trace(message: => String): WriterT[F, G[LogMessage], Unit] =
        build(traceEnabled, LogLevel.Trace, None, message)

      override def debug(t: Throwable)(message: => String): WriterT[F, G[LogMessage], Unit] =
        build(debugEnabled, LogLevel.Debug, t.some, message)
      override def debug(message: => String): WriterT[F, G[LogMessage], Unit] =
        build(debugEnabled, LogLevel.Debug, None, message)

      override def info(t: Throwable)(message: => String): WriterT[F, G[LogMessage], Unit] =
        build(infoEnabled, LogLevel.Info, t.some, message)
      override def info(message: => String): WriterT[F, G[LogMessage], Unit] =
        build(infoEnabled, LogLevel.Info, None, message)

      override def warn(t: Throwable)(message: => String): WriterT[F, G[LogMessage], Unit] =
        build(warnEnabled, LogLevel.Warn, t.some, message)
      override def warn(message: => String): WriterT[F, G[LogMessage], Unit] =
        build(warnEnabled, LogLevel.Warn, None, message)

      override def error(t: Throwable)(message: => String): WriterT[F, G[LogMessage], Unit] =
        build(errorEnabled, LogLevel.Error, t.some, message)
      override def error(message: => String): WriterT[F, G[LogMessage], Unit] =
        build(errorEnabled, LogLevel.Error, None, message)

      private def isEnabled(enabled: Boolean): WriterT[F, G[LogMessage], Boolean] =
        WriterT.liftF[F, G[LogMessage], Boolean](Applicative[F].pure(enabled))

      private def build(
          enabled: Boolean,
          level: LogLevel,
          t: Option[Throwable],
          message: => String
      ): WriterT[F, G[LogMessage], Unit] =
        if (enabled)
          WriterT.tell[F, G[LogMessage]](Applicative[G].pure(LogMessage(level, t, message)))
        else WriterT.value[F, G[LogMessage], Unit](())

      private implicit val monoidGLogMessage: Monoid[G[LogMessage]] =
        Alternative[G].algebra[LogMessage]
    }

  def run[F[_]: Monad, G[_]: Foldable](l: Logger[F]): WriterT[F, G[LogMessage], *] ~> F =
    new (WriterT[F, G[LogMessage], *] ~> F) {
      override def apply[A](fa: WriterT[F, G[LogMessage], A]): F[A] =
        fa.run.flatMap { case (toLog, out) =>
          toLog.traverse_(LogMessage.log(_, l)).as(out)
        }
    }
}
