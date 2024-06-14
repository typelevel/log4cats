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

import cats.data.WriterT
import cats.kernel.Monoid
import cats.syntax.all._
import cats.{~>, Alternative, Applicative, Foldable, Monad}
import org.typelevel.log4cats.{SelfAwareStructuredLogger, StructuredLogger}

/**
 * A `SelfAwareStructuredLogger` implemented using `cats.data.WriterT`.
 *
 * >>> WARNING: READ BEFORE USAGE! <<<
 * https://github.com/typelevel/log4cats/blob/main/core/shared/src/main/scala/org/typelevel/log4cats/extras/README.md
 * >>> WARNING: READ BEFORE USAGE! <<<
 *
 * If a `SelfAwareStructuredLogger` is needed for test code, the `testing` module provides a better
 * option: `org.typelevel.log4cats.testing.StructuredTestingLogger`
 */
object WriterTStructuredLogger {
  def apply[F[_]: Applicative, G[_]: Alternative](
      traceEnabled: Boolean = true,
      debugEnabled: Boolean = true,
      infoEnabled: Boolean = true,
      warnEnabled: Boolean = true,
      errorEnabled: Boolean = true
  ): SelfAwareStructuredLogger[WriterT[F, G[StructuredLogMessage], *]] =
    new SelfAwareStructuredLogger[WriterT[F, G[StructuredLogMessage], *]] {
      type LoggerF[A] = WriterT[F, G[StructuredLogMessage], A]

      private implicit val monoidGLogMessage: Monoid[G[StructuredLogMessage]] =
        Alternative[G].algebra[StructuredLogMessage]

      private def shouldLog(ll: LogLevel): Boolean = ll match {
        case LogLevel.Error => errorEnabled
        case LogLevel.Warn => warnEnabled
        case LogLevel.Info => infoEnabled
        case LogLevel.Debug => debugEnabled
        case LogLevel.Trace => traceEnabled
      }

      private def build(
          level: LogLevel,
          ctx: Map[String, String],
          t: Option[Throwable],
          message: => String
      ): LoggerF[Unit] =
        Applicative[LoggerF].whenA(shouldLog(level)) {
          WriterT.tell[F, G[StructuredLogMessage]](Applicative[G].pure {
            StructuredLogMessage(level, ctx, t, message)
          })
        }

      override def isEnabled(ll: LogLevel): WriterT[F, G[StructuredLogMessage], Boolean] =
        WriterT.liftF[F, G[StructuredLogMessage], Boolean](Applicative[F].pure(shouldLog(ll)))

      override def log(ll: LogLevel, msg: => String): WriterT[F, G[StructuredLogMessage], Unit] =
        build(ll, Map.empty, none, msg)

      override def log(
          ll: LogLevel,
          t: Throwable,
          msg: => String
      ): WriterT[F, G[StructuredLogMessage], Unit] =
        build(ll, Map.empty, t.some, msg)

      override def log(
          ll: LogLevel,
          ctx: Map[String, String],
          msg: => String
      ): WriterT[F, G[StructuredLogMessage], Unit] =
        build(ll, ctx, none, msg)

      override def log(
          ll: LogLevel,
          ctx: Map[String, String],
          t: Throwable,
          msg: => String
      ): WriterT[F, G[StructuredLogMessage], Unit] =
        build(ll, ctx, t.some, msg)
    }

  def run[F[_]: Monad, G[_]: Foldable](
      l: StructuredLogger[F]
  ): WriterT[F, G[StructuredLogMessage], *] ~> F =
    new ~>[WriterT[F, G[StructuredLogMessage], *], F] {
      override def apply[A](fa: WriterT[F, G[StructuredLogMessage], A]): F[A] =
        fa.run.flatMap { case (toLog, out) =>
          toLog.traverse_(StructuredLogMessage.log(_, l)).as(out)
        }
    }
}
