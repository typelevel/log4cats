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

package object syntax {

  implicit final class LoggerFactorySyntax[F[_]](
      private val factory: LoggerFactory[F]
  ) extends AnyVal {

    /**
     * Creates a new [[LoggerFactory]] that returns [[SelfAwareStructuredLogger]] that adds
     * information captured by `Contextual` to the context.
     *
     * @example
     *   {{{
     * import org.typelevel.log4cats.mtl.syntax._
     *
     * case class LogContext(logId: String)
     *
     * implicit val toLogContext: ToContext[LogContext] =
     *   ctx => Map("log_id" -> ctx.logId)
     *
     * implicit val askLogContext: Ask[F, LogContext] = ???
     *
     * val loggerFactory: LoggerFactory[F] = ??? // the general factory, e.g. Slf4jFactory
     * val contextual: LoggerFactory[F] = loggerFactory.contextual
     *   }}}
     */
    def contextual(implicit C: Contextual[F], F: FlatMap[F]): LoggerFactory[F] =
      ContextualLoggerFactory(factory)

  }

  implicit final class SelfAwareStructuredLoggerSyntax[F[_]](
      private val logger: SelfAwareStructuredLogger[F]
  ) extends AnyVal {

    /**
     * Creates a new [[SelfAwareStructuredLogger]] that adds information captured by `Contextual` to
     * the context.
     *
     * @example
     *   {{{
     * import org.typelevel.log4cats.mtl.syntax._
     *
     * case class LogContext(logId: String)
     *
     * implicit val toLogContext: ToContext[LogContext] =
     *   ctx => Map("log_id" -> ctx.logId)
     *
     * implicit val askLogContext: Ask[F, LogContext] = ???
     *
     * val logger: SelfAwareStructuredLogger[F] = ??? // the general logger, e.g. Slf4jLogger
     * val contextual: SelfAwareStructuredLogger[F] = logger.contextual
     *   }}}
     */
    def contextual(implicit C: Contextual[F], F: FlatMap[F]): SelfAwareStructuredLogger[F] =
      ContextualSelfAwareStructuredLogger[F](logger)

  }

  implicit final class StructuredLoggerSyntax[F[_]](
      private val logger: StructuredLogger[F]
  ) extends AnyVal {

    /**
     * Creates a new [[StructuredLogger]] that adds information captured by `Contextual` to the
     * context.
     *
     * @example
     *   {{{
     * import org.typelevel.log4cats.mtl.syntax._
     *
     * case class LogContext(logId: String)
     *
     * implicit val toLogContext: ToContext[LogContext] =
     *   ctx => Map("log_id" -> ctx.logId)
     *
     * implicit val askLogContext: Ask[F, LogContext] = ???
     *
     * val logger: StructuredLogger[F] = ??? // the general logger, e.g. Slf4jLogger
     * val contextual: StructuredLogger[F] = logger.contextual
     *   }}}
     */
    def contextual(implicit C: Contextual[F], F: FlatMap[F]): StructuredLogger[F] =
      ContextualStructuredLogger[F](logger)

  }

}
