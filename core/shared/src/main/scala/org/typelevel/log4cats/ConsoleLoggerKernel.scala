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

import cats.effect.kernel.Sync

/**
 * A simple console implementation of LoggerKernel for testing the SAM design.
 */
class ConsoleLoggerKernel[F[_], Ctx](implicit F: Sync[F]) extends LoggerKernel[F, Ctx] {

  def log(level: KernelLogLevel, record: Log.Builder[Ctx] => Log.Builder[Ctx]): F[Unit] = {
    F.delay {
      val logRecord = record(Log.mutableBuilder[Ctx]()).build()

      val timestamp = logRecord.timestamp.map(_.toMillis).getOrElse(System.currentTimeMillis())
      // Use simple timestamp formatting instead of java.time.Instant for Scala Native compatibility
      val timeStr = s"${new java.util.Date(timestamp).toString}"

      val levelStr = logRecord.level.namePadded
      val message = logRecord.message
      val className = logRecord.className.map(c => s"[$c]").getOrElse("")
      val fileName =
        logRecord.fileName.map(f => s"($f:${logRecord.line.getOrElse(0)})").getOrElse("")

      val contextStr = if (logRecord.context.nonEmpty) {
        val contextPairs = logRecord.context.map { case (k, v) => s"$k=$v" }.mkString(", ")
        s" {$contextPairs}"
      } else ""

      val throwableStr = logRecord.throwable.map(t => s"\n${t.toString}").getOrElse("")

      val logLine = s"$timeStr $levelStr $className$fileName$contextStr $message$throwableStr"

      println(logLine)
    }
  }
}

object ConsoleLoggerKernel {
  def apply[F[_], Ctx](implicit F: Sync[F]): ConsoleLoggerKernel[F, Ctx] =
    new ConsoleLoggerKernel[F, Ctx]
}
