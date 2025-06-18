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
 * This allows to capture several elements in vararg-based interface methods, enriching a single log
 * with various pieces of information.
 *
 * This allows for an interesting UX, where the details of the encoding of some data into a log can
 * be separate from the actual log statements.
 */
trait LogRecord extends (Log.Builder => Log.Builder)

object LogRecord {
  def combine(all: Seq[LogRecord]): LogRecord = Combined(all)

  implicit def toLogRecord[A: Recordable](value: => A): LogRecord =
    Recordable[A].record(value)

  private case class Combined(all: Seq[LogRecord]) extends LogRecord {
    def apply(record: Log.Builder): Log.Builder = {
      var current = record
      all.foreach { logBit =>
        current = logBit(current)
      }
      current
    }
  }
}
