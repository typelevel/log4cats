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

final case class LogLevel(name: String, value: Double) {
  def namePadded: String = LogLevel.padded(this)

  LogLevel.add(this)
}

object LogLevel {
  private var maxLength = 0

  private var map = Map.empty[String, LogLevel]
  private var padded = Map.empty[LogLevel, String]

  implicit final val LevelOrdering: Ordering[LogLevel] =
    Ordering.by[LogLevel, Double](_.value).reverse

  val Trace: LogLevel = LogLevel("TRACE", 100.0)
  val Debug: LogLevel = LogLevel("DEBUG", 200.0)
  val Info: LogLevel = LogLevel("INFO", 300.0)
  val Warn: LogLevel = LogLevel("WARN", 400.0)
  val Error: LogLevel = LogLevel("ERROR", 500.0)
  val Fatal: LogLevel = LogLevel("FATAL", 600.0)

  def add(level: LogLevel): Unit = synchronized {
    val length = level.name.length
    map += level.name.toLowerCase -> level
    if (length > maxLength) {
      maxLength = length
      padded = map.map { case (_, level) =>
        level -> level.name.padTo(maxLength, ' ').mkString
      }
    } else {
      padded += level -> level.name.padTo(maxLength, ' ').mkString
    }
  }

  def get(name: String): Option[LogLevel] = map.get(name.toLowerCase)

  def apply(name: String): LogLevel = get(name).getOrElse(
    throw new RuntimeException(s"Level not found by name: $name")
  )
}
