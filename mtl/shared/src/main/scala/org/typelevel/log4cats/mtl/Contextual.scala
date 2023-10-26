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

package org.typelevel.log4cats.mtl

import cats.Applicative
import cats.mtl.Ask

/**
 * Represents the ability to access a contextual environment as a map of key-value pairs.
 *
 * @example
 *   {{{
 * case class LogContext(logId: String)
 *
 * // how to transform the contextual environment into the log context
 * implicit val toLogContext: ToContext[LogContext] =
 *   ctx => Map("log_id" -> ctx.logId)
 *
 * // available out of the box for Kleisli, Reader, etc
 * implicit val askLogContext: Ask[F, LogContext] = ???
 *
 * implicit val contextual: Contextual[F] = Contextual.fromAsk
 *   }}}
 *
 * @tparam F
 *   the higher-kinded type of a polymorphic effect
 */
@scala.annotation.implicitNotFound("""
Couldn't find `Contextual` for type `${F}`. Make sure you have the following implicit instances:
1) `org.typelevel.log4cats.mtl.ToContext[Ctx]`
2) `cats.mtl.Ask[${F}, Ctx]` 
""")
trait Contextual[F[_]] {

  /**
   * Retrieves the current contextual environment as a map of key-value pairs.
   */
  def current: F[Map[String, String]]
}

object Contextual {

  def apply[F[_]](implicit ev: Contextual[F]): Contextual[F] = ev

  /**
   * Creates a [[Contextual]] instance that always returns the given `ctx`.
   */
  def const[F[_]: Applicative](ctx: Map[String, String]): Contextual[F] =
    new Contextual[F] {
      val current: F[Map[String, String]] = Applicative[F].pure(ctx)
    }

  implicit def fromAsk[F[_], A: ToContext](implicit A: Ask[F, A]): Contextual[F] =
    new Contextual[F] {
      def current: F[Map[String, String]] = A.reader(ToContext[A].extract)
    }

}
