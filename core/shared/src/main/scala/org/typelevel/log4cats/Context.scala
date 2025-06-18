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
import org.typelevel.log4cats.JsonLike.Aux

/** A value that can be written into a json-like construct, provided a visitor.
  */
trait Context {
  def capture[J](jsonLike: JsonLike.Aux[J]): J
}

object Context {
  trait Encoder[A] {
    def encode[J](json: JsonLike.Aux[J], a: A): J
  }
  
  object Encoder {
    def apply[A](implicit ev: Encoder[A]): ev.type = ev

    implicit val stringEncoder: Encoder[String] = new Encoder[String] {
      def encode[J](json: JsonLike.Aux[J], a: String) = json.string(a)
    }

    implicit val intEncoder: Encoder[Int] = new Encoder[Int] {
      def encode[J](json: JsonLike.Aux[J], a: Int) = json.int(a)
    }

    implicit val booleanEncoder: Encoder[Boolean] = new Encoder[Boolean] {
      def encode[J](json: Aux[J], a: Boolean): J = json.bool(a)
    }

    implicit val timestampEncoder: Encoder[FiniteDuration] =
      new Encoder[FiniteDuration] {
        def encode[J](json: JsonLike.Aux[J], a: FiniteDuration) =
          json.timestamp(a)
      }
  }

  implicit def toContext[A: Encoder](a: A): Context =
    DeferredRecord(a, Encoder[A])

  private case class DeferredRecord[A](a: A, encoder: Encoder[A])
      extends Context {
    def capture[J](jsonLike: JsonLike.Aux[J]): J = encoder.encode(jsonLike, a)
  }
} 