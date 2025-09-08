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

/**
 * Typeclass representing the notion that a value can contribute to a log, by transforming it in
 * some way.
 */
trait Recordable[Ctx, A] {
  def record(value: => A): LogRecord[Ctx]
}

object Recordable {
  def apply[Ctx, A](implicit ev: Recordable[Ctx, A]): ev.type = ev

  implicit def stringLoggable[Ctx]: Recordable[Ctx, String] = new Recordable[Ctx, String] {
    def record(value: => String) = _.withMessage(value)
  }

  implicit def contextPairLoggable[Ctx, A](implicit
      encoder: Context.Encoder[A, Ctx]
  ): Recordable[Ctx, (String, A)] =
    new Recordable[Ctx, (String, A)] {
      override def record(value: => (String, A)): LogRecord[Ctx] = {
        val (k, v) = value
        (_: Log.Builder[Ctx]).withContext(k)(v)
      }
    }

  implicit def throwableLoggable[Ctx, T <: Throwable]: Recordable[Ctx, T] =
    new Recordable[Ctx, T] {
      def record(value: => T): LogRecord[Ctx] = _.withThrowable(value)
    }

  implicit def intLoggable[Ctx](implicit
      encoder: Context.Encoder[Int, Ctx]
  ): Recordable[Ctx, Int] =
    new Recordable[Ctx, Int] {
      def record(value: => Int): LogRecord[Ctx] = {
        val v = value
        (builder: Log.Builder[Ctx]) => builder.withContext("value")(v)
      }
    }

  implicit def longLoggable[Ctx](implicit
      encoder: Context.Encoder[Long, Ctx]
  ): Recordable[Ctx, Long] =
    new Recordable[Ctx, Long] {
      def record(value: => Long): LogRecord[Ctx] = {
        val v = value
        (builder: Log.Builder[Ctx]) => builder.withContext("value")(v)
      }
    }

  implicit def doubleLoggable[Ctx](implicit
      encoder: Context.Encoder[Double, Ctx]
  ): Recordable[Ctx, Double] =
    new Recordable[Ctx, Double] {
      def record(value: => Double): LogRecord[Ctx] = {
        val v = value
        (builder: Log.Builder[Ctx]) => builder.withContext("value")(v)
      }
    }

  implicit def booleanLoggable[Ctx](implicit
      encoder: Context.Encoder[Boolean, Ctx]
  ): Recordable[Ctx, Boolean] =
    new Recordable[Ctx, Boolean] {
      def record(value: => Boolean): LogRecord[Ctx] = {
        val v = value
        (builder: Log.Builder[Ctx]) => builder.withContext("value")(v)
      }
    }

  implicit def mapLoggable[Ctx, A](implicit
      encoder: Context.Encoder[A, Ctx]
  ): Recordable[Ctx, Map[String, A]] =
    new Recordable[Ctx, Map[String, A]] {
      def record(value: => Map[String, A]): LogRecord[Ctx] = {
        val map = value
        (builder: Log.Builder[Ctx]) => builder.withContextMap(map)
      }
    }
}
