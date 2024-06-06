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
