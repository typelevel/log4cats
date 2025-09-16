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

import org.typelevel.log4cats.Context.Encoder

/**
 * A value that can be written into a json-like construct, provided a visitor.
 */
trait Context[C] {
  def capture[A](a: A)(implicit E: Encoder[A, C]): C
}

object Context {
  trait Encoder[A, B] {
    def encode(a: A): B
  }

  object Encoder {
    def apply[A, B](implicit ev: Encoder[A, B]): ev.type = ev

    // Identity encoder for when input and output types are the same
    implicit def identityEncoder[A]: Encoder[A, A] = a => a

    implicit val stringToStringEncoder: Encoder[String, String] = a => a

    implicit val intToStringEncoder: Encoder[Int, String] = _.toString

    implicit val longToStringEncoder: Encoder[Long, String] = _.toString

    implicit val doubleToStringEncoder: Encoder[Double, String] = _.toString

    implicit val booleanToStringEncoder: Encoder[Boolean, String] = if (_) "true" else "false"

    // Removed Instant encoder for Scala Native compatibility
    // implicit val instantToStringEncoder: Encoder[Instant, String] =
    //   DateTimeFormatter.ISO_INSTANT.format(_)

    implicit val finiteDurationToStringEncoder: Encoder[FiniteDuration, String] = _.toString
  }
}
