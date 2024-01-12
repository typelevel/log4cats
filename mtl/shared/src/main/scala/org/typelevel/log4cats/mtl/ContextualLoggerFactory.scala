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
import cats.syntax.functor._
import org.typelevel.log4cats.mtl.syntax._

object ContextualLoggerFactory {

  /**
   * Creates a new [[LoggerFactory]] that returns [[SelfAwareStructuredLogger]] that adds
   * information captured by `Contextual` to the context.
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
   * val loggerFactory: LoggerFactory[F] = ??? // the general factory, e.g. Slf4jFactory
   * val contextual: LoggerFactory[F] = ContextualLoggerFactory(loggerFactory)
   *   }}}
   */
  def apply[F[_]: Contextual: FlatMap](factory: LoggerFactory[F]): LoggerFactory[F] =
    new LoggerFactory[F] {
      def getLoggerFromName(name: String): SelfAwareStructuredLogger[F] =
        factory.getLoggerFromName(name).contextual

      def fromName(name: String): F[SelfAwareStructuredLogger[F]] =
        factory.fromName(name).map(logger => logger.contextual)
    }
}
