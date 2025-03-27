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

import cats.data.Chain

/**
 * A `Logger` that does not immediately log.
 *
 * >>> WARNING: READ BEFORE USAGE! <<<
 * https://github.com/typelevel/log4cats/blob/main/core/shared/src/main/scala/org/typelevel/log4cats/extras/README.md
 * >>> WARNING: READ BEFORE USAGE! <<<
 */
trait DeferredLogging[F[_]] {

  /**
   * View the logs in the buffer.
   *
   * This is primarily useful for testing, and will not effect the behavior of calls to `log`
   */
  def inspect: F[Chain[DeferredLogMessage]]

  /**
   * Log the deferred messages
   *
   * This may be called multiple times, and each log should only be logged once.
   */
  def log: F[Unit]
}
