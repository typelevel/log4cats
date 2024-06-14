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
import cats.kernel.Monoid
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
      type LoggerF[A] = WriterT[F, G[LogMessage], A]

      private implicit val monoidGLogMessage: Monoid[G[LogMessage]] =
        Alternative[G].algebra[LogMessage]

      private def shouldLog(ll: LogLevel): Boolean = ll match {
        case LogLevel.Error => errorEnabled
        case LogLevel.Warn => warnEnabled
        case LogLevel.Info => infoEnabled
        case LogLevel.Debug => debugEnabled
        case LogLevel.Trace => traceEnabled
      }

      private def build(level: LogLevel, t: Option[Throwable], message: => String): LoggerF[Unit] =
        Applicative[LoggerF].whenA(shouldLog(level)) {
          WriterT.tell[F, G[LogMessage]](Applicative[G].pure {
            LogMessage(level, t, message)
          })
        }

      override def isEnabled(ll: LogLevel): WriterT[F, G[LogMessage], Boolean] =
        WriterT.liftF[F, G[LogMessage], Boolean](Applicative[F].pure(shouldLog(ll)))

      override def log(
          ll: LogLevel,
          t: Throwable,
          msg: => String
      ): WriterT[F, G[LogMessage], Unit] =
        build(ll, t.some, msg)

      override def log(ll: LogLevel, msg: => String): WriterT[F, G[LogMessage], Unit] =
        build(ll, none, msg)
    }

  def run[F[_]: Monad, G[_]: Foldable](l: Logger[F]): WriterT[F, G[LogMessage], *] ~> F =
    new (WriterT[F, G[LogMessage], *] ~> F) {
      override def apply[A](fa: WriterT[F, G[LogMessage], A]): F[A] =
        fa.run.flatMap { case (toLog, out) =>
          toLog.traverse_(LogMessage.log(_, l)).as(out)
        }
    }
}
