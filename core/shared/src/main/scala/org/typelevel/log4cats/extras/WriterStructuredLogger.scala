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

import cats.data.Writer
import cats.syntax.all._
import cats.{~>, Alternative, Applicative, Foldable, Id}
import org.typelevel.log4cats.{SelfAwareStructuredLogger, StructuredLogger}

/**
 * A `SelfAwareStructuredLogger` implemented using `cats.data.Writer`.
 *
 * If a `SelfAwareStructuredLogger` is needed for test code, the `testing` module provides a better
 * option: `org.typelevel.log4cats.testing.StructuredTestingLogger`
 */
object WriterStructuredLogger {
  def apply[G[_]: Alternative](
      traceEnabled: Boolean = true,
      debugEnabled: Boolean = true,
      infoEnabled: Boolean = true,
      warnEnabled: Boolean = true,
      errorEnabled: Boolean = true
  ): SelfAwareStructuredLogger[Writer[G[StructuredLogMessage], *]] =
    WriterTStructuredLogger[Id, G](
      traceEnabled,
      debugEnabled,
      infoEnabled,
      warnEnabled,
      errorEnabled
    )

  def run[F[_]: Applicative, G[_]: Foldable](
      l: StructuredLogger[F]
  ): Writer[G[StructuredLogMessage], *] ~> F =
    new ~>[Writer[G[StructuredLogMessage], *], F] {
      def apply[A](fa: Writer[G[StructuredLogMessage], A]): F[A] = {
        val (toLog, out) = fa.run
        toLog.traverse_(StructuredLogMessage.log(_, l)).as(out)
      }
    }
}
