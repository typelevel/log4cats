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

import cats._
import cats.data._
import cats.syntax.all._
import org.typelevel.log4cats._

/**
 * A `SelfAwareLogger` implemented using `cats.data.Writer`.
 *
 * >>> WARNING: READ BEFORE USAGE! <<<
 * https://github.com/typelevel/log4cats/blob/main/core/shared/src/main/scala/org/typelevel/log4cats/extras/README.md
 * >>> WARNING: READ BEFORE USAGE! <<<
 *
 * If a `SelfAwareLogger` is needed for test code, the `testing` module provides a better option:
 * `org.typelevel.log4cats.testing.TestingLogger`
 */
object WriterLogger {

  def apply[G[_]: Alternative](
      traceEnabled: Boolean = true,
      debugEnabled: Boolean = true,
      infoEnabled: Boolean = true,
      warnEnabled: Boolean = true,
      errorEnabled: Boolean = true
  ): SelfAwareLogger[Writer[G[LogMessage], *]] =
    WriterTLogger[cats.Id, G](traceEnabled, debugEnabled, infoEnabled, warnEnabled, errorEnabled)

  def run[F[_]: Applicative, G[_]: Foldable](l: Logger[F]): Writer[G[LogMessage], *] ~> F =
    new(Writer[G[LogMessage], *] ~> F) {
      def apply[A](fa: Writer[G[LogMessage], A]): F[A] = {
        val (toLog, out) = fa.run
        toLog.traverse_(LogMessage.log(_, l)).as(out)
      }
    }
}
