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

package org.typelevel.log4cats.extras

import java.io.{ByteArrayOutputStream, PrintStream}
import java.time.Instant
import java.time.format.DateTimeFormatter

trait ConsoleLogFormat {
  def format(loggerName: String, level: LogLevel, timestamp: Instant, msg: String): String
  def format(
      loggerName: String,
      level: LogLevel,
      timestamp: Instant,
      msg: String,
      throwable: Throwable
  ): String
  def format(
      loggerName: String,
      level: LogLevel,
      timestamp: Instant,
      msg: String,
      ctx: Map[String, String]
  ): String
  def format(
      loggerName: String,
      level: LogLevel,
      timestamp: Instant,
      msg: String,
      ctx: Map[String, String],
      throwable: Throwable
  ): String
}
object ConsoleLogFormat {
  val Default: ConsoleLogFormat = new ConsoleLogFormat {
    private def fmt(i: Instant): String = DateTimeFormatter.ISO_INSTANT.format(i)
    private def fmt(ctx: Map[String, String]): String = {
      val builder = new StringBuilder()
      ctx.toVector.foreach { case (key, value) =>
        builder.append("  ").append(key).append(" = ").append(value).append("\n")
      }
      builder.result()
    }
    private def fmt(t: Throwable): String = {
      val baos = new ByteArrayOutputStream()
      val ps = new PrintStream(baos)
      t.printStackTrace(ps)
      baos.toString.linesIterator.map(s => s"  $s").mkString("\n")
    }

    override def format(n: String, l: LogLevel, ts: Instant, m: String): String =
      s"${fmt(ts)} $l $n - $m"

    override def format(n: String, l: LogLevel, ts: Instant, m: String, t: Throwable): String =
      s"${fmt(ts)} $l $n - $m\n${fmt(t)}"

    override def format(
        n: String,
        l: LogLevel,
        ts: Instant,
        m: String,
        ctx: Map[String, String]
    ): String =
      if (ctx.isEmpty) format(n, l, ts, m)
      else s"${fmt(ts)} $l $n - $m\n${fmt(ctx)}"

    override def format(
        n: String,
        l: LogLevel,
        ts: Instant,
        m: String,
        ctx: Map[String, String],
        t: Throwable
    ): String =
      if (ctx.isEmpty) format(n, l, ts, m, t)
      else s"${fmt(ts)} $l $n - $m\n${fmt(ctx)}\t${fmt(t)}"
  }
}
