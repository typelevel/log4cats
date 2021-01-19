/*
 * Copyright 2018 Christopher Davenport
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
import cats.data._
import cats.syntax.all._
import org.typelevel.log4cats._

/**
 * >>> WARNING READ BEFORE USAGE! <<<
 * This logger will NOT log anything if `F` fails!
 *
 * Running the `WriterT` instance will yield a value of type `F[(G[LogMessage], A)]`.
 * As a result, the logged messages can be materialized if and only `F` succeeds.
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
    new ~>[WriterT[F, G[LogMessage], *], F] {
      override def apply[A](fa: WriterT[F, G[LogMessage], A]): F[A] = {
        def logMessage(logMessage: LogMessage): F[Unit] = logMessage match {
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

        fa.run.flatMap { case (toLog, out) =>
          toLog.traverse_(logMessage).as(out)
        }
      }
    }
}
