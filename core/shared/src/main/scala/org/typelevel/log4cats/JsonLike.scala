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

import scala.concurrent.duration.FiniteDuration

/** A visitor-like construct that allows for capturing contextual values of
  * several types, without enforcing an in-memory representation or a third-party dependency.
  */
trait JsonLike {
  type J

  def nul: J
  def bool(value: Boolean): J
  def int(value: Int): J
  def short(value: Int): J
  def long(value: Int): J
  def double(value: Double): J
  def timestamp(ts: FiniteDuration): J
  def string(value: String): J
  def obj(bindings: (String, J)*): J
  def arr(elems: J*): J
}

object JsonLike {
  type Aux[Json] = JsonLike {
    type J = Json
  }
} 