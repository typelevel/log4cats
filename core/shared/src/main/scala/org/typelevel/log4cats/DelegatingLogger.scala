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

import cats.*
import cats.syntax.all.*

/**
 * A logger that delegates all logging operations to another logger that is wrapped in an effect
 * F[_].
 *
 * This is useful when you need to modify the underlying logger for each log operation. A common use
 * case is adding the current tracing context. For example, if your application uses Natchez, you
 * might have something like:
 * {{{
 * def tracedLogger[F[_]](logger: SelfAwareStructuredLogger[F], trace: Trace[F]): SelfAwareStructuredLogger[F] =
 *   new DelegatingLogger(
 *     (trace.traceId, trace.spanId).mapN { (traceId, spanId) =>
 *       logger.addContext(Map(
 *         "trace.id" -> traceId.toString,
 *         "span.id" -> spanId.toString
 *       ))
 *     }
 *   )
 * }}}
 *
 * @param delegate
 *   The effect containing the logger to delegate to. This effect is evaluated for each logging
 *   operation.
 * @tparam F
 *   The effect type, which must have a FlatMap instance
 */
class DelegatingLogger[F[_]: FlatMap](delegate: F[SelfAwareStructuredLogger[F]])
    extends SelfAwareStructuredLogger[F] {
  override def isTraceEnabled: F[Boolean] = delegate.flatMap(_.isTraceEnabled)
  override def isDebugEnabled: F[Boolean] = delegate.flatMap(_.isDebugEnabled)
  override def isInfoEnabled: F[Boolean] = delegate.flatMap(_.isInfoEnabled)
  override def isWarnEnabled: F[Boolean] = delegate.flatMap(_.isWarnEnabled)
  override def isErrorEnabled: F[Boolean] = delegate.flatMap(_.isErrorEnabled)

  override def trace(ctx: Map[String, String])(msg: => String): F[Unit] =
    delegate.flatMap(_.trace(ctx)(msg))
  override def trace(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
    delegate.flatMap(_.trace(ctx, t)(msg))
  override def debug(ctx: Map[String, String])(msg: => String): F[Unit] =
    delegate.flatMap(_.debug(ctx)(msg))
  override def debug(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
    delegate.flatMap(_.debug(ctx, t)(msg))
  override def info(ctx: Map[String, String])(msg: => String): F[Unit] =
    delegate.flatMap(_.info(ctx)(msg))
  override def info(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
    delegate.flatMap(_.info(ctx, t)(msg))
  override def warn(ctx: Map[String, String])(msg: => String): F[Unit] =
    delegate.flatMap(_.warn(ctx)(msg))
  override def warn(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
    delegate.flatMap(_.warn(ctx, t)(msg))
  override def error(ctx: Map[String, String])(msg: => String): F[Unit] =
    delegate.flatMap(_.error(ctx)(msg))
  override def error(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
    delegate.flatMap(_.error(ctx, t)(msg))
  override def error(t: Throwable)(message: => String): F[Unit] =
    delegate.flatMap(_.error(t)(message))
  override def warn(t: Throwable)(message: => String): F[Unit] =
    delegate.flatMap(_.warn(t)(message))
  override def info(t: Throwable)(message: => String): F[Unit] =
    delegate.flatMap(_.info(t)(message))
  override def debug(t: Throwable)(message: => String): F[Unit] =
    delegate.flatMap(_.debug(t)(message))
  override def trace(t: Throwable)(message: => String): F[Unit] =
    delegate.flatMap(_.trace(t)(message))
  override def error(message: => String): F[Unit] = delegate.flatMap(_.error(message))
  override def warn(message: => String): F[Unit] = delegate.flatMap(_.warn(message))
  override def info(message: => String): F[Unit] = delegate.flatMap(_.info(message))
  override def debug(message: => String): F[Unit] = delegate.flatMap(_.debug(message))
  override def trace(message: => String): F[Unit] = delegate.flatMap(_.trace(message))
}
