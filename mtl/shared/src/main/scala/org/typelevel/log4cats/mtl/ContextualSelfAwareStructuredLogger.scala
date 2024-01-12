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
package mtl

import cats.FlatMap
import cats.syntax.flatMap._
import cats.syntax.functor._

private[mtl] class ContextualSelfAwareStructuredLogger[F[_]: Contextual: FlatMap](
    sl: SelfAwareStructuredLogger[F]
) extends SelfAwareStructuredLogger[F] {

  private val defaultCtx: F[Map[String, String]] = Contextual[F].current

  private def modify(ctx: Map[String, String]): F[Map[String, String]] =
    defaultCtx.map(c => c ++ ctx)

  def isTraceEnabled: F[Boolean] = sl.isTraceEnabled
  def isDebugEnabled: F[Boolean] = sl.isDebugEnabled
  def isInfoEnabled: F[Boolean] = sl.isInfoEnabled
  def isWarnEnabled: F[Boolean] = sl.isWarnEnabled
  def isErrorEnabled: F[Boolean] = sl.isErrorEnabled

  def error(message: => String): F[Unit] =
    defaultCtx.flatMap(sl.error(_)(message))

  def warn(message: => String): F[Unit] =
    defaultCtx.flatMap(sl.warn(_)(message))

  def info(message: => String): F[Unit] =
    defaultCtx.flatMap(sl.info(_)(message))

  def debug(message: => String): F[Unit] =
    defaultCtx.flatMap(sl.debug(_)(message))

  def trace(message: => String): F[Unit] =
    defaultCtx.flatMap(sl.trace(_)(message))

  def error(t: Throwable)(message: => String): F[Unit] =
    defaultCtx.flatMap(sl.error(_, t)(message))

  def warn(t: Throwable)(message: => String): F[Unit] =
    defaultCtx.flatMap(sl.warn(_, t)(message))

  def info(t: Throwable)(message: => String): F[Unit] =
    defaultCtx.flatMap(sl.info(_, t)(message))

  def debug(t: Throwable)(message: => String): F[Unit] =
    defaultCtx.flatMap(sl.debug(_, t)(message))

  def trace(t: Throwable)(message: => String): F[Unit] =
    defaultCtx.flatMap(sl.trace(_, t)(message))

  def trace(ctx: Map[String, String])(msg: => String): F[Unit] =
    modify(ctx).flatMap(sl.trace(_)(msg))

  def debug(ctx: Map[String, String])(msg: => String): F[Unit] =
    modify(ctx).flatMap(sl.debug(_)(msg))

  def info(ctx: Map[String, String])(msg: => String): F[Unit] =
    modify(ctx).flatMap(sl.info(_)(msg))

  def warn(ctx: Map[String, String])(msg: => String): F[Unit] =
    modify(ctx).flatMap(sl.warn(_)(msg))

  def error(ctx: Map[String, String])(msg: => String): F[Unit] =
    modify(ctx).flatMap(sl.error(_)(msg))

  def error(ctx: Map[String, String], t: Throwable)(message: => String): F[Unit] =
    modify(ctx).flatMap(sl.error(_, t)(message))

  def warn(ctx: Map[String, String], t: Throwable)(message: => String): F[Unit] =
    modify(ctx).flatMap(sl.warn(_, t)(message))

  def info(ctx: Map[String, String], t: Throwable)(message: => String): F[Unit] =
    modify(ctx).flatMap(sl.info(_, t)(message))

  def debug(ctx: Map[String, String], t: Throwable)(message: => String): F[Unit] =
    modify(ctx).flatMap(sl.debug(_, t)(message))

  def trace(ctx: Map[String, String], t: Throwable)(message: => String): F[Unit] =
    modify(ctx).flatMap(sl.trace(_, t)(message))
}

object ContextualSelfAwareStructuredLogger {

  /**
   * Creates a new [[SelfAwareStructuredLogger]] that adds information captured by `Contextual` to
   * the context.
   *
   * @example
   *   {{{
   * case class LogContext(logId: String)
   *
   * implicit val toLogContext: ToContext[LogContext] =
   *   ctx => Map("log_id" -> ctx.logId)
   *
   * implicit val askLogContext: Ask[F, LogContext] = ???
   *
   * val logger: SelfAwareStructuredLogger[F] = ??? // the general logger, e.g. Slf4jLogger
   * val contextual: SelfAwareStructuredLogger[F] = ContextualSelfAwareStructuredLogger(logger)
   *   }}}
   */
  def apply[F[_]: Contextual: FlatMap](
      logger: SelfAwareStructuredLogger[F]
  ): SelfAwareStructuredLogger[F] =
    new ContextualSelfAwareStructuredLogger[F](logger)

}
