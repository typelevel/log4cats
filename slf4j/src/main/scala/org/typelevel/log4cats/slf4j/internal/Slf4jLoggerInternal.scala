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

import org.typelevel.log4cats.*
import cats.syntax.all.*
import cats.effect.*
import org.slf4j.Logger as JLogger
import org.slf4j.MDC

import scala.annotation.nowarn

private[slf4j] object Slf4jLoggerInternal {

  final val singletonsByName = true
  final val trailingDollar = false

  sealed trait LevelLogger[F[_]] extends Any {
    def isEnabled: F[Boolean]

    def apply(msg: => String): F[Unit]
    def apply(t: Throwable)(msg: => String): F[Unit]
  }

  private[this] def contextLog[F[_]](
      isEnabled: F[Boolean],
      ctxF: F[Map[String, String]],
      logging: () => Unit
  )(implicit F: Sync[F]): F[Unit] = {

    val ifEnabled = ctxF.flatMap(ctx =>
      F.delay {
        val backup =
          try MDC.getCopyOfContextMap()
          catch {
            case e: IllegalStateException =>
              // MDCAdapter is missing, no point in doing anything with
              // the MDC, so just hope the logging backend can salvage
              // something.
              logging()
              throw e
          }

        try {
          // Once 2.12 is no longer supported, change this to MDC.setContextMap(ctx.asJava)
          MDC.clear()
          ctx.foreach { case (k, v) => MDC.put(k, v) }
          logging()
        } finally
          if (backup eq null) MDC.clear()
          else MDC.setContextMap(backup)
      }
    )

    isEnabled.ifM(
      ifEnabled,
      F.unit
    )
  }

  @nowarn("msg=used")
  final class Slf4jLogger[F[_]](
      val logger: JLogger,
      sync: Sync.Type = Sync.Type.Delay,
      defaultCtx: F[Map[String, String]]
  )(implicit F: Sync[F])
      extends SelfAwareStructuredLogger[F] {

    @deprecated("Use constructor with sync", "2.6.0")
    def this(logger: JLogger, sync: Sync.Type)(F: Sync[F]) =
      this(logger, Sync.Type.Delay, F.pure(Map.empty))(F)

    @deprecated("Use constructor with sync", "2.6.0")
    def this(logger: JLogger)(F: Sync[F]) =
      this(logger, Sync.Type.Delay)(F)

    override def isTraceEnabled: F[Boolean] = F.delay(logger.isTraceEnabled)
    override def isDebugEnabled: F[Boolean] = F.delay(logger.isDebugEnabled)
    override def isInfoEnabled: F[Boolean] = F.delay(logger.isInfoEnabled)
    override def isWarnEnabled: F[Boolean] = F.delay(logger.isWarnEnabled)
    override def isErrorEnabled: F[Boolean] = F.delay(logger.isErrorEnabled)

    override def trace(t: Throwable)(msg: => String): F[Unit] =
      contextLog(isTraceEnabled, defaultCtx, () => logger.trace(msg, t))
    override def trace(msg: => String): F[Unit] =
      contextLog(isTraceEnabled, defaultCtx, () => logger.trace(msg))
    override def trace(ctx: Map[String, String])(msg: => String): F[Unit] =
      contextLog(isTraceEnabled, defaultCtx.map(_ ++ ctx), () => logger.trace(msg))
    override def trace(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
      contextLog(isTraceEnabled, defaultCtx.map(_ ++ ctx), () => logger.trace(msg, t))

    override def debug(t: Throwable)(msg: => String): F[Unit] =
      contextLog(isDebugEnabled, defaultCtx, () => logger.debug(msg, t))
    override def debug(msg: => String): F[Unit] =
      contextLog(isDebugEnabled, defaultCtx, () => logger.debug(msg))
    override def debug(ctx: Map[String, String])(msg: => String): F[Unit] =
      contextLog(isDebugEnabled, defaultCtx.map(_ ++ ctx), () => logger.debug(msg))
    override def debug(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
      contextLog(isDebugEnabled, defaultCtx.map(_ ++ ctx), () => logger.debug(msg, t))

    override def info(t: Throwable)(msg: => String): F[Unit] =
      contextLog(isInfoEnabled, defaultCtx, () => logger.info(msg, t))
    override def info(msg: => String): F[Unit] =
      contextLog(isInfoEnabled, defaultCtx, () => logger.info(msg))
    override def info(ctx: Map[String, String])(msg: => String): F[Unit] =
      contextLog(isInfoEnabled, defaultCtx.map(_ ++ ctx), () => logger.info(msg))
    override def info(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
      contextLog(isInfoEnabled, defaultCtx.map(_ ++ ctx), () => logger.info(msg, t))

    override def warn(t: Throwable)(msg: => String): F[Unit] =
      contextLog(isWarnEnabled, defaultCtx, () => logger.warn(msg, t))
    override def warn(msg: => String): F[Unit] =
      contextLog(isWarnEnabled, defaultCtx, () => logger.warn(msg))
    override def warn(ctx: Map[String, String])(msg: => String): F[Unit] =
      contextLog(isWarnEnabled, defaultCtx.map(_ ++ ctx), () => logger.warn(msg))
    override def warn(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
      contextLog(isWarnEnabled, defaultCtx.map(_ ++ ctx), () => logger.warn(msg, t))

    override def error(t: Throwable)(msg: => String): F[Unit] =
      contextLog(isErrorEnabled, defaultCtx, () => logger.error(msg, t))
    override def error(msg: => String): F[Unit] =
      contextLog(isErrorEnabled, defaultCtx, () => logger.error(msg))
    override def error(ctx: Map[String, String])(msg: => String): F[Unit] =
      contextLog(isErrorEnabled, defaultCtx.map(_ ++ ctx), () => logger.error(msg))
    override def error(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
      contextLog(isErrorEnabled, defaultCtx.map(_ ++ ctx), () => logger.error(msg, t))
  }
}
