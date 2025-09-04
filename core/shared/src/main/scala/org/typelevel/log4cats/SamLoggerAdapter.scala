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

/**
 * Adapter layer to bridge the new SAM LoggerKernel with the existing Logger interface. This allows
 * gradual migration from the old multi-method design to the new SAM design.
 *
 * ## Usage
 *
 * ### Converting SAM LoggerKernel to old Logger interface
 * ```scala
 * val kernel = Slf4jLoggerKernel.fromName[IO]("MyApp")
 * val oldLogger = SamLoggerAdapter.toLogger(kernel)
 *
 * // Use old API
 * oldLogger.info("Works with old API")
 * oldLogger.error(exception)("Works with old API")
 * ```
 *
 * ### Converting old Logger to SAM LoggerKernel
 * ```scala
 * val oldLogger = Slf4jLogger.getLogger[IO]
 * val samLogger = SamLoggerAdapter.loggerToSamLogger(oldLogger)
 *
 * // Use new SAM API
 * samLogger.info("Works with new API", ("key", "value"))
 * ```
 *
 * ## Limitations
 *
 *   - **Context Loss**: When converting from LoggerKernel → Logger → LoggerKernel, structured
 *     context information (key-value pairs) is lost because the old Logger interface doesn't
 *     support structured logging. Only message and throwable information is preserved.
 *   - **Performance**: The `toLoggerKernel` method creates a full log record to extract message and
 *     throwable, which has some overhead.
 *
 * ## Best Practices
 *
 *   - Use `toLogger` for backward compatibility when migrating to new backends
 *   - Use `loggerToSamLogger` when you need structured logging capabilities
 *   - Avoid round-trip conversions (LoggerKernel → Logger → LoggerKernel) as they lose context
 */
object SamLoggerAdapter {

  /**
   * Convert a LoggerKernel to the existing Logger interface.
   *
   * This is the primary use case for the adapter - allowing new SAM-based backends to work with
   * existing code that uses the old Logger interface.
   *
   * **Note**: Structured context information is not accessible through the old Logger interface, so
   * any context added via the SAM API will not be visible when using the returned Logger.
   */
  def toLogger[F[_], Ctx](kernel: LoggerKernel[F, Ctx]): Logger[F] = new Logger[F] {
    def error(message: => String): F[Unit] =
      kernel.log(KernelLogLevel.Error, _.withMessage(message))

    def error(t: Throwable)(message: => String): F[Unit] =
      kernel.log(KernelLogLevel.Error, _.withMessage(message).withThrowable(t))

    def warn(message: => String): F[Unit] =
      kernel.log(KernelLogLevel.Warn, _.withMessage(message))

    def warn(t: Throwable)(message: => String): F[Unit] =
      kernel.log(KernelLogLevel.Warn, _.withMessage(message).withThrowable(t))

    def info(message: => String): F[Unit] =
      kernel.log(KernelLogLevel.Info, _.withMessage(message))

    def info(t: Throwable)(message: => String): F[Unit] =
      kernel.log(KernelLogLevel.Info, _.withMessage(message).withThrowable(t))

    def debug(message: => String): F[Unit] =
      kernel.log(KernelLogLevel.Debug, _.withMessage(message))

    def debug(t: Throwable)(message: => String): F[Unit] =
      kernel.log(KernelLogLevel.Debug, _.withMessage(message).withThrowable(t))

    def trace(message: => String): F[Unit] =
      kernel.log(KernelLogLevel.Trace, _.withMessage(message))

    def trace(t: Throwable)(message: => String): F[Unit] =
      kernel.log(KernelLogLevel.Trace, _.withMessage(message).withThrowable(t))
  }

  /**
   * Convert the existing Logger interface to a LoggerKernel.
   *
   * **Important**: This conversion has limitations:
   *   - Only message and throwable information is preserved
   *   - All structured context (key-value pairs) is lost because the old Logger interface doesn't
   *     support structured logging
   *   - This method is primarily intended for testing and round-trip compatibility
   *
   * For production use, prefer using the SAM LoggerKernel directly or use `toLogger` to convert
   * from LoggerKernel to Logger.
   */
  def toLoggerKernel[F[_], Ctx](logger: Logger[F]): LoggerKernel[F, Ctx] =
    new LoggerKernel[F, Ctx] {
      def log(level: KernelLogLevel, record: Log.Builder[Ctx] => Log.Builder[Ctx]): F[Unit] = {
        // Build the log record to extract message and throwable
        // Note: Context information is intentionally not preserved as the old Logger
        // interface doesn't support structured logging
        val logRecord = record(Log.mutableBuilder[Ctx]()).build()
        val message = logRecord.message()
        val throwable = logRecord.throwable

        // Route to appropriate logger method based on level and throwable presence
        (level, throwable) match {
          case (KernelLogLevel.Error, Some(t)) => logger.error(t)(message)
          case (KernelLogLevel.Error, None) => logger.error(message)
          case (KernelLogLevel.Warn, Some(t)) => logger.warn(t)(message)
          case (KernelLogLevel.Warn, None) => logger.warn(message)
          case (KernelLogLevel.Info, Some(t)) => logger.info(t)(message)
          case (KernelLogLevel.Info, None) => logger.info(message)
          case (KernelLogLevel.Debug, Some(t)) => logger.debug(t)(message)
          case (KernelLogLevel.Debug, None) => logger.debug(message)
          case (KernelLogLevel.Trace, Some(t)) => logger.trace(t)(message)
          case (KernelLogLevel.Trace, None) => logger.trace(message)
          case _ => logger.info(message) // fallback for unknown levels
        }
      }
    }

  /**
   * Convert a SamLogger to the existing Logger interface.
   *
   * This is a convenience method that delegates to `toLogger`.
   */
  def samLoggerToLogger[F[_], Ctx](samLogger: SamLogger[F, Ctx]): Logger[F] = toLogger(samLogger)

  /**
   * Convert the existing Logger interface to a SamLogger.
   *
   * This allows old Logger implementations to be used with the new SAM API, enabling structured
   * logging capabilities.
   *
   * **Note**: The underlying Logger implementation must support the old interface. Any structured
   * context added via the SAM API will be lost when the underlying Logger processes the log (since
   * it only receives message and throwable).
   */
  def loggerToSamLogger[F[_], Ctx](logger: Logger[F]): SamLogger[F, Ctx] =
    SamLogger.wrap(toLoggerKernel(logger))
}
