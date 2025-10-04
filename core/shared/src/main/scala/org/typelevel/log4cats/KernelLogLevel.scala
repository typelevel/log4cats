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

import cats.Order

final case class KernelLogLevel(name: String, value: Int) {
  def namePadded: String = name.padTo(5, ' ').mkString
}

object KernelLogLevel {
  implicit final val orderKernelLogLevel: Order[KernelLogLevel] =
    Order.by[KernelLogLevel, Int](-_.value)

  val Trace: KernelLogLevel = KernelLogLevel("TRACE", 100)
  val Debug: KernelLogLevel = KernelLogLevel("DEBUG", 200)
  val Info: KernelLogLevel = KernelLogLevel("INFO", 300)
  val Warn: KernelLogLevel = KernelLogLevel("WARN", 400)
  val Error: KernelLogLevel = KernelLogLevel("ERROR", 500)
  val Fatal: KernelLogLevel = KernelLogLevel("FATAL", 600)
}
