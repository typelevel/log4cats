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

package org.typelevel.log4cats

import cats.~>

/**
 * This is the fundamental abstraction: a single-abstract-method interface that has the following
 * properties:
 *
 *   - Doesn't enforce a specific memory layout. More specific interfaces can back `LogRecord` with
 *     mutables/immutables values, and avoid storing things that are not important.
 *   - LogRecord allows to precisely capture a lot of information. In particular, it does not
 *     enforce a `Map[String, String]` representation of context values that is not sufficient to
 *     leverage all the power from logging backends query engines, and without pulling a third-party
 *     JSON library.
 *   - The SAM-like nature of the construct makes it inherently middleware friendly, as a single
 *     method call needs to be intercepted/proxied in order to amend the behaviour of the logger.
 *
 * This also means that different libraries can use wrappers on top of this kernel interface to use
 * whatever UX is preferred without necessarily imposing constraints on the underlying
 * implementation.
 */
trait LoggerKernel[F[_], Ctx] {
  def log(level: KernelLogLevel, record: Log.Builder[Ctx] => Log.Builder[Ctx]): F[Unit]

  /**
   * Transform the effect type using a natural transformation. This allows converting between
   * different effect types (e.g., IO to Task, Task to IO).
   */
  def mapK[G[_]](f: F ~> G): LoggerKernel[G, Ctx] = new LoggerKernel[G, Ctx] {
    def log(level: KernelLogLevel, record: Log.Builder[Ctx] => Log.Builder[Ctx]): G[Unit] =
      f(LoggerKernel.this.log(level, record))
  }
}
