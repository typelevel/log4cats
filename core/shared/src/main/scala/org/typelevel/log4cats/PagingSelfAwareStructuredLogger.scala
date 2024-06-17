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

import cats.*
import cats.effect.std.UUIDGen
import cats.syntax.all.*
import org.typelevel.log4cats.extras.LogLevel

import java.io.{PrintWriter, StringWriter}
import java.util.UUID

object PagingSelfAwareStructuredLogger {

  /**
   * Wrap a SelfAwareStructuredLogger adding pagination functionality to avoid the truncation (hard
   * splitting) by the underline logging provider. For the functions with a Throwable parameter, the
   * stack trace is appended to the message before pagination.
   *
   * The actual text to be logged has 3 parts:
   *
   * Part 1. The chunk of message in each page; Part 2. This function will add the page header and
   * footer; Part 3. The base class SelfAwareStructuredLogger may add tracing data etc logging
   * context.
   *
   * The total of the above 3 parts should be smaller than the log size limit of the underline
   * logging provider to avoid hard splitting.
   *
   * Example: Assume the log size limit of the underline logging provider is 75 Kb, setting
   * pageSizeK to 64 leaves 11 Kb for (part 2) and (part 3).
   *
   * @param pageSizeK
   *   The size (in unit kibibyte) of the chunk of message in each page.
   * @param maxPageNeeded
   *   The maximum number of pages to be logged.
   * @param logger
   *   The SelfAwareStructuredLogger to be used to do the actual logging.
   * @tparam F
   *   Effect type class.
   * @return
   *   SelfAwareStructuredLogger with pagination.
   */
  def paged[F[_]: Monad: UUIDGen](pageSizeK: Int = 64, maxPageNeeded: Int = 999)(
      logger: SelfAwareStructuredLogger[F]
  ): SelfAwareStructuredLogger[F] =
    new PagingSelfAwareStructuredLogger[F](pageSizeK, maxPageNeeded, UUIDGen.randomUUID)(logger)

  @deprecated("Use paged", "2.5.0")
  def withPaging[F[_]: Monad](pageSizeK: Int = 64, maxPageNeeded: Int = 999)(
      logger: SelfAwareStructuredLogger[F]
  ): SelfAwareStructuredLogger[F] =
    new PagingSelfAwareStructuredLogger[F](pageSizeK, maxPageNeeded)(logger)

  private class PagingSelfAwareStructuredLogger[F[_]: Monad](
      pageSizeK: Int,
      maxPageNeeded: Int,
      randomUUID: F[UUID]
  )(
      sl: SelfAwareStructuredLogger[F]
  ) extends SelfAwareStructuredLogger[F] {
    if (pageSizeK <= 0 || maxPageNeeded <= 0)
      throw new IllegalArgumentException(
        s"pageSizeK(=$pageSizeK) and maxPageNeeded(=$maxPageNeeded) must be positive numbers."
      )

    @deprecated("Use constructor with randomUUID", "2.5.0")
    def this(pageSizeK: Int, maxPageNeeded: Int)(
        sl: SelfAwareStructuredLogger[F]
    ) =
      this(pageSizeK, maxPageNeeded, Monad[F].unit.map(_ => UUID.randomUUID()))(sl)

    private val pageIndices = (1 to maxPageNeeded).toList
    private val logSplitIdN = "log_split_id"
    private val pageSize = pageSizeK * 1024

    private def pagedLogging(
        logOpWithCtx: Map[String, String] => (=> String) => F[Unit],
        ctx: Map[String, String],
        logSplitId: String,
        msg: String
    ): F[Unit] = {
      val numOfPagesRaw = (msg.length - 1) / pageSize + 1
      val numOfPages = Math.min(numOfPagesRaw, maxPageNeeded)
      if (numOfPages <= 1) sl.log(logLevel, addPageCtx(msg, 1, 1, ctx), msg)
      else {
        val logSplitIdPart1 = logSplitId.split('-').head
        val pageHeaderTail = s"$numOfPages $logSplitIdPart1"
        val pageFooterTail = s"$numOfPages $logSplitIdN=$logSplitId page_size=$pageSizeK Kib"
        pageIndices
          .take(numOfPages)
          .traverse_ { pi =>
            val beginIndex = (pi - 1) * pageSize
            val pageContent = msg.slice(beginIndex, beginIndex + pageSize)

            val page = show"""Page $pi/$pageHeaderTail
                            |
                            |$pageContent
                            |
                            |Page $pi/$pageFooterTail""".stripMargin

            sl.log(logLevel, addPageCtx(page, pi, numOfPages, ctx), page)
          }
      }
    }

    private def addMsgCtx(msg: String, ctx: Map[String, String]): F[(String, Map[String, String])] =
      randomUUID.map { uuid =>
        val logSplitId = uuid.show
        val msgLength = s"${msg.length}"
        (
          logSplitId,
          ctx
            .updated(logSplitIdN, logSplitId)
            .updated("page_size", s"$pageSizeK Kib")
            .updated("whole_message_size_bytes", msgLength)
            // The following is deprecated
            .updated("log_size", s"$msgLength Byte")
        )
      }

    private def addPageCtx(
        page: String,
        pageNum: Int,
        totalPages: Int,
        ctx: Map[String, String]
    ): Map[String, String] =
      ctx
        .updated("total_pages", s"$totalPages")
        .updated("page_num", s"$pageNum")
        .updated("log_size_bytes", s"${page.length}")

    private def doLogging(logLevel: LogLevel, ctx: Map[String, String], msg: => String): F[Unit] =
      sl.isEnabled(logLevel)
        .ifM(
          {
            val cachedMsg = msg
            addMsgCtx(cachedMsg, ctx).flatMap { case (logSplitId, newCtx) =>
              pagedLogging(logLevel, newCtx, logSplitId, cachedMsg)
            }
          },
          Applicative[F].unit
        )

    private def doLoggingThrowable(
       logLevel: LogLevel,
       ctx: Map[String, String],
       t: Throwable,
       msg: => String
    ): F[Unit] = doLogging(logLevel, ctx, s"$msg\n${getStackTrace(t)}")

    def getStackTrace(t: Throwable): String = {
      val sw = new StringWriter()
      val pw = new PrintWriter(sw, true)
      t.printStackTrace(pw)
      sw.getBuffer.toString
    }

    override def isEnabled(ll: LogLevel): F[Boolean] = sl.isEnabled(ll)

    override def log(ll: LogLevel, msg: => String): F[Unit] =
      doLogging(ll, Map.empty, msg)

    override def log(ll: LogLevel, t: Throwable, msg: => String): F[Unit] =
      doLoggingThrowable(ll, Map.empty, t, msg)

    override def log(ll: LogLevel, ctx: Map[String, String], msg: => String): F[Unit] =
      doLogging(ll, ctx, msg)

    override def log(
                      ll: LogLevel,
                      ctx: Map[String, String],
                      t: Throwable,
                      msg: => String
                    ): F[Unit] =
      doLoggingThrowable(ll, ctx, t, msg)
  }
}
