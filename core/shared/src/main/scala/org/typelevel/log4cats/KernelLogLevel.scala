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

import cats.{Order, Show}
import cats.kernel.{Eq, Hash}

sealed abstract class KernelLogLevel(val name: String, val value: Int) {
  def namePadded: String = name.padTo(5, ' ').mkString

  override def toString: String = name
}

object KernelLogLevel {
  implicit final val log4catsCatsInstances: Eq[KernelLogLevel] & Hash[KernelLogLevel] & Order[KernelLogLevel] & Show[KernelLogLevel] =
    new Hash[KernelLogLevel] with Order[KernelLogLevel] with Show[KernelLogLevel] {
      override def eqv(x: KernelLogLevel, y: KernelLogLevel): Boolean = x == y

      override def hash(x: KernelLogLevel): Int = x.##

      override def compare(x: KernelLogLevel, y: KernelLogLevel): Int = x.value.compare(y.value)

      override def show(t: KernelLogLevel): String = t.name
    }

  case object Trace extends KernelLogLevel("TRACE", 100)
  case object Debug extends KernelLogLevel("DEBUG", 200)
  case object Info extends KernelLogLevel("INFO", 300)
  case object Warn extends KernelLogLevel("WARN", 400)
  case object Error extends KernelLogLevel("ERROR", 500)
  case object Fatal extends KernelLogLevel("FATAL", 600)
}
