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

package org.typelevel.log4cats.noop

import cats.Applicative
import org.typelevel.log4cats.{KernelLogLevel, Log, LoggerKernel}

object NoOpLoggerKernel {
  def apply[F[_]: Applicative, Ctx]: LoggerKernel[F, Ctx] = new LoggerKernel[F, Ctx] {
    override def log(level: KernelLogLevel, record: Log.Builder[Ctx] => Log.Builder[Ctx]): F[Unit] =
      Applicative[F].unit
  }

  def strict[F[_]: Applicative, Ctx]: LoggerKernel[F, Ctx] = new LoggerKernel[F, Ctx] {
    override def log(
        level: KernelLogLevel,
        record: Log.Builder[Ctx] => Log.Builder[Ctx]
    ): F[Unit] = {
      val _ = record(Log.strictNoOpBuilder[Ctx]())
      Applicative[F].unit
    }
  }
}
