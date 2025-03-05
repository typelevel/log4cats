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
      if (numOfPages <= 1)
        logOpWithCtx(addPageCtx(msg, 1, 1, ctx))(msg)
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

            logOpWithCtx(addPageCtx(page, pi, numOfPages, ctx))(page)
          }
      }
    }

    private def addMsgCtx(
        msg: String,
        ctx: Map[String, String]
    ): F[(String, Map[String, String])] =
      randomUUID.map { uuid =>
        val logSplitId = uuid.show
        val msgLength = msg.length
        (
          logSplitId,
          ctx
            .updated(logSplitIdN, logSplitId)
            .updated("page_size", s"$pageSizeK Kib")
            .updated("whole_message_size_bytes", s"$msgLength")
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

    private def doLogging(
        loggingLevelChk: => F[Boolean],
        logOpWithCtx: Map[String, String] => (=> String) => F[Unit],
        msg: => String,
        ctx: Map[String, String] = Map()
    ): F[Unit] = {
      loggingLevelChk.ifM(
        {
          // At this point we know we're going to log and/or interact
          // with msg, so we materialize the message here so we don't
          // materialize it multiple times
          val materializedMsg = msg
          addMsgCtx(materializedMsg, ctx).flatMap { case (logSplitId, newCtx) =>
            pagedLogging(logOpWithCtx, newCtx, logSplitId, materializedMsg)
          }
        },
        Applicative[F].unit
      )
    }

    private def doLoggingThrowable(
        loggingLevelChk: => F[Boolean],
        logOpWithCtx: Map[String, String] => (=> String) => F[Unit],
        t: Throwable,
        msg: => String,
        ctx: Map[String, String] = Map()
    ): F[Unit] = {
      loggingLevelChk.ifM(
        doLogging(loggingLevelChk, logOpWithCtx, s"$msg\n${getStackTrace(t)}", ctx),
        Applicative[F].unit
      )
    }

    def getStackTrace(t: Throwable): String = {
      val sw = new StringWriter()
      val pw = new PrintWriter(sw, true)
      t.printStackTrace(pw)
      sw.getBuffer.toString
    }

    override def isTraceEnabled: F[Boolean] = sl.isTraceEnabled

    override def isDebugEnabled: F[Boolean] = sl.isDebugEnabled

    override def isInfoEnabled: F[Boolean] = sl.isInfoEnabled

    override def isWarnEnabled: F[Boolean] = sl.isWarnEnabled

    override def isErrorEnabled: F[Boolean] = sl.isErrorEnabled

    // Log message

    override def trace(msg: => String): F[Unit] =
      doLogging(isTraceEnabled, sl.trace, msg)

    override def debug(msg: => String): F[Unit] =
      doLogging(isDebugEnabled, sl.debug, msg)

    override def info(msg: => String): F[Unit] =
      doLogging(isInfoEnabled, sl.info, msg)

    override def warn(msg: => String): F[Unit] =
      doLogging(isWarnEnabled, sl.warn, msg)

    override def error(msg: => String): F[Unit] =
      doLogging(isErrorEnabled, sl.error, msg)

    // Log message and throwable

    override def trace(t: Throwable)(msg: => String): F[Unit] =
      doLoggingThrowable(isTraceEnabled, sl.trace, t, msg)

    override def debug(t: Throwable)(msg: => String): F[Unit] =
      doLoggingThrowable(isDebugEnabled, sl.debug, t, msg)

    override def info(t: Throwable)(msg: => String): F[Unit] =
      doLoggingThrowable(isInfoEnabled, sl.info, t, msg)

    override def warn(t: Throwable)(msg: => String): F[Unit] =
      doLoggingThrowable(isWarnEnabled, sl.warn, t, msg)

    override def error(t: Throwable)(msg: => String): F[Unit] =
      doLoggingThrowable(isErrorEnabled, sl.error, t, msg)

    // Log message, passing context

    override def trace(ctx: Map[String, String])(msg: => String): F[Unit] =
      doLogging(isTraceEnabled, sl.trace, msg, ctx)

    override def debug(ctx: Map[String, String])(msg: => String): F[Unit] =
      doLogging(isDebugEnabled, sl.debug, msg, ctx)

    override def info(ctx: Map[String, String])(msg: => String): F[Unit] =
      doLogging(isInfoEnabled, sl.info, msg, ctx)

    override def warn(ctx: Map[String, String])(msg: => String): F[Unit] =
      doLogging(isWarnEnabled, sl.warn, msg, ctx)

    override def error(ctx: Map[String, String])(msg: => String): F[Unit] =
      doLogging(isErrorEnabled, sl.error, msg, ctx)

    // Log message and throwable, passing context

    override def trace(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
      doLoggingThrowable(isTraceEnabled, sl.trace, t, msg, ctx)

    override def debug(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
      doLoggingThrowable(isDebugEnabled, sl.debug, t, msg, ctx)

    override def info(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
      doLoggingThrowable(isInfoEnabled, sl.info, t, msg, ctx)

    override def warn(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
      doLoggingThrowable(isWarnEnabled, sl.warn, t, msg, ctx)

    override def error(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
      doLoggingThrowable(isErrorEnabled, sl.error, t, msg, ctx)
  }
}
