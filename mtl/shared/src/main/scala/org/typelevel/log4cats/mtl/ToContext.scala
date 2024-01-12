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

/**
 * Creates context information from the value `A`.
 *
 * @example
 *   {{{
 * case class LogContext(logId: String)
 *
 * implicit val toLogContext: ToContext[LogContext] =
 *   ctx => Map("log_id" -> ctx.logId)
 *   }}}
 */
trait ToContext[A] {
  def extract(a: A): Map[String, String]
}

object ToContext {
  def apply[A](implicit ev: ToContext[A]): ToContext[A] = ev

  implicit val toContextFromMap: ToContext[Map[String, String]] =
    (a: Map[String, String]) => a

}
