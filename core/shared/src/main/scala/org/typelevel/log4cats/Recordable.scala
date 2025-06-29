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

  implicit def tupleLoggable[Ctx, T](implicit
      ev: T =:= Ctx
  ): Recordable[Ctx, (String, T)] =
    new Recordable[Ctx, (String, T)] {
      override def record(value: => (String, T)): LogRecord[Ctx] = {
        val (k, v) = value
        (_: Log.Builder[Ctx]).withContext(k)(ev(v))
      }
    }

  // Special case for (String, String) when Ctx is Context
  implicit def stringTupleLoggable: Recordable[Context, (String, String)] =
    new Recordable[Context, (String, String)] {
      override def record(value: => (String, String)): LogRecord[Context] = {
        val (k, v) = value
        (_: Log.Builder[Context]).withContext(k)(v: Context)
      }
    }

  // Special case for (String, Int) when Ctx is Context
  implicit def intTupleLoggable: Recordable[Context, (String, Int)] =
    new Recordable[Context, (String, Int)] {
      override def record(value: => (String, Int)): LogRecord[Context] = {
        val (k, v) = value
        (_: Log.Builder[Context]).withContext(k)(v: Context)
      }
    }

  implicit def throwableLoggable[Ctx, T <: Throwable]: Recordable[Ctx, T] =
    new Recordable[Ctx, T] {
      def record(value: => T): LogRecord[Ctx] = _.withThrowable(value)
    }
}
