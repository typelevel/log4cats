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

package org.typelevel.log4cats.slf4j.internal

import cats.effect.*
import org.slf4j.{Logger as JLogger, MDC}
import org.typelevel.log4cats.*

private[slf4j] object Slf4jLoggerInternal {

  final val singletonsByName = true
  final val trailingDollar = false

  sealed trait LevelLogger[F[_]] extends Any {
    def isEnabled: F[Boolean]

    def apply(msg: => String): F[Unit]
    def apply(t: Throwable)(msg: => String): F[Unit]
  }

  final class Slf4jLogger[F[_]](
      val logger: JLogger,
      sync: Sync.Type,
      defaultCtx: Map[String, String]
  )(implicit
      F: Sync[F]
  ) extends SelfAwareStructuredLogger[F] {

    def this(logger: JLogger, sync: Sync.Type = Sync.Type.Delay)(F: Sync[F]) =
      this(logger, sync, Map.empty)(F)

    @deprecated("Use constructor with sync", "2.6.0")
    def this(logger: JLogger)(
        F: Sync[F]
    ) =
      this(logger, Sync.Type.Delay)(F)

    protected def kernel: LoggerKernel[F, String] = new LoggerKernel[F, String] {
      def log(
          level: KernelLogLevel,
          logBuilder: Log.Builder[String] => Log.Builder[String]
      ): F[Unit] = {
        // Check if the log level is enabled before building the Log object
        val isEnabled = level match {
          case KernelLogLevel.Trace => F.delay(logger.isTraceEnabled)
          case KernelLogLevel.Debug => F.delay(logger.isDebugEnabled)
          case KernelLogLevel.Info => F.delay(logger.isInfoEnabled)
          case KernelLogLevel.Warn => F.delay(logger.isWarnEnabled)
          case KernelLogLevel.Error => F.delay(logger.isErrorEnabled)
          case _ => F.pure(false) // Handle any other KernelLogLevel values
        }

        Sync[F].flatMap(isEnabled) { enabled =>
          if (enabled) {
            // Only build the Log object if the level is enabled
            val log = logBuilder(Log.mutableBuilder[String]()).build()
            val context = log.context

            // Set MDC context
            val setMdc = F.delay {
              context.foreach { case (k, v) => MDC.put(k, v) }
            }

            val clearMdc = F.delay {
              context.keys.foreach(MDC.remove)
            }

            val logMessage = level match {
              case KernelLogLevel.Trace =>
                F.delay {
                  val message = log.message()
                  val throwable = log.throwable
                  if (throwable.isDefined) logger.trace(message, throwable.get)
                  else logger.trace(message)
                }
              case KernelLogLevel.Debug =>
                F.delay {
                  val message = log.message()
                  val throwable = log.throwable
                  if (throwable.isDefined) logger.debug(message, throwable.get)
                  else logger.debug(message)
                }
              case KernelLogLevel.Info =>
                F.delay {
                  val message = log.message()
                  val throwable = log.throwable
                  if (throwable.isDefined) logger.info(message, throwable.get)
                  else logger.info(message)
                }
              case KernelLogLevel.Warn =>
                F.delay {
                  val message = log.message()
                  val throwable = log.throwable
                  if (throwable.isDefined) logger.warn(message, throwable.get)
                  else logger.warn(message)
                }
              case KernelLogLevel.Error =>
                F.delay {
                  val message = log.message()
                  val throwable = log.throwable
                  if (throwable.isDefined) logger.error(message, throwable.get)
                  else logger.error(message)
                }
              case _ =>
                F.delay {
                  val message = log.message()
                  val throwable = log.throwable
                  if (throwable.isDefined) logger.error(message, throwable.get)
                  else logger.error(message)
                }
            }

            Sync[F].flatMap(setMdc)(_ => Sync[F].flatMap(logMessage)(_ => clearMdc))
          } else {
            // If level is disabled, do nothing
            F.unit
          }
        }
      }
    }

    private val isTraceEnabledUnsafe = () => logger.isTraceEnabled
    private val isDebugEnabledUnsafe = () => logger.isDebugEnabled
    private val isInfoEnabledUnsafe = () => logger.isInfoEnabled
    private val isWarnEnabledUnsafe = () => logger.isWarnEnabled
    private val isErrorEnabledUnsafe = () => logger.isErrorEnabled

    override def addContext(ctx: Map[String, String]): SelfAwareStructuredLogger[F] =
      new Slf4jLogger[F](logger, sync, defaultCtx ++ ctx)

    override def isTraceEnabled: F[Boolean] =
      F.delay(withPreparedMDCUnsafe(Map.empty, isTraceEnabledUnsafe))
    override def isDebugEnabled: F[Boolean] =
      F.delay(withPreparedMDCUnsafe(Map.empty, isDebugEnabledUnsafe))
    override def isInfoEnabled: F[Boolean] =
      F.delay(withPreparedMDCUnsafe(Map.empty, isInfoEnabledUnsafe))
    override def isWarnEnabled: F[Boolean] =
      F.delay(withPreparedMDCUnsafe(Map.empty, isWarnEnabledUnsafe))
    override def isErrorEnabled: F[Boolean] =
      F.delay(withPreparedMDCUnsafe(Map.empty, isErrorEnabledUnsafe))

    override def trace(t: Throwable)(msg: => String): F[Unit] =
      noContextLog(isTraceEnabledUnsafe, () => logger.trace(msg, t))
    override def trace(msg: => String): F[Unit] =
      noContextLog(isTraceEnabledUnsafe, () => logger.trace(msg))
    override def trace(ctx: Map[String, String])(msg: => String): F[Unit] =
      contextLog(isTraceEnabledUnsafe, ctx, () => logger.trace(msg))
    override def trace(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
      contextLog(isTraceEnabledUnsafe, ctx, () => logger.trace(msg, t))

    override def debug(t: Throwable)(msg: => String): F[Unit] =
      noContextLog(isDebugEnabledUnsafe, () => logger.debug(msg, t))
    override def debug(msg: => String): F[Unit] =
      noContextLog(isDebugEnabledUnsafe, () => logger.debug(msg))
    override def debug(ctx: Map[String, String])(msg: => String): F[Unit] =
      contextLog(isDebugEnabledUnsafe, ctx, () => logger.debug(msg))
    override def debug(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
      contextLog(isDebugEnabledUnsafe, ctx, () => logger.debug(msg, t))

    override def info(t: Throwable)(msg: => String): F[Unit] =
      noContextLog(isInfoEnabledUnsafe, () => logger.info(msg, t))
    override def info(msg: => String): F[Unit] =
      noContextLog(isInfoEnabledUnsafe, () => logger.info(msg))
    override def info(ctx: Map[String, String])(msg: => String): F[Unit] =
      contextLog(isInfoEnabledUnsafe, ctx, () => logger.info(msg))
    override def info(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
      contextLog(isInfoEnabledUnsafe, ctx, () => logger.info(msg, t))

    override def warn(t: Throwable)(msg: => String): F[Unit] =
      noContextLog(isWarnEnabledUnsafe, () => logger.warn(msg, t))
    override def warn(msg: => String): F[Unit] =
      noContextLog(isWarnEnabledUnsafe, () => logger.warn(msg))
    override def warn(ctx: Map[String, String])(msg: => String): F[Unit] =
      contextLog(isWarnEnabledUnsafe, ctx, () => logger.warn(msg))
    override def warn(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
      contextLog(isWarnEnabledUnsafe, ctx, () => logger.warn(msg, t))

    override def error(t: Throwable)(msg: => String): F[Unit] =
      noContextLog(isErrorEnabledUnsafe, () => logger.error(msg, t))
    override def error(msg: => String): F[Unit] =
      noContextLog(isErrorEnabledUnsafe, () => logger.error(msg))
    override def error(ctx: Map[String, String])(msg: => String): F[Unit] =
      contextLog(isErrorEnabledUnsafe, ctx, () => logger.error(msg))
    override def error(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
      contextLog(isErrorEnabledUnsafe, ctx, () => logger.error(msg, t))

    private def withPreparedMDCUnsafe[A](extraCtx: Map[String, String], body: () => A): A = {
      val ctx = defaultCtx ++ extraCtx
      val backup =
        try MDC.getCopyOfContextMap()
        catch {
          case e: IllegalStateException =>
            // MDCAdapter is missing, no point in doing anything with
            // the MDC, so just hope the logging backend can salvage
            // something.
            body()
            throw e
        }

      try {
        // Once 2.12 is no longer supported, change this to MDC.setContextMap(ctx.asJava)
        MDC.clear()
        ctx.foreach { case (k, v) => MDC.put(k, v) }
        body()
      } finally
        if (backup eq null) MDC.clear()
        else MDC.setContextMap(backup)
    }

    private def noContextLog(isEnabledUnsafe: () => Boolean, logging: () => Unit): F[Unit] =
      contextLog(isEnabledUnsafe, Map.empty, logging)

    private def contextLog(
        isEnabledUnsafe: () => Boolean,
        ctx: Map[String, String],
        logging: () => Unit
    ): F[Unit] =
      F.delay(
        withPreparedMDCUnsafe(
          ctx,
          () =>
            if (isEnabledUnsafe()) {
              logging()
            }
        )
      )
  }
}
