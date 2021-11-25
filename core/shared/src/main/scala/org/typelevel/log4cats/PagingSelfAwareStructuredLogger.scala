/*
 * Copyright 2018 Christopher Davenport
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

import cats._
import cats.implicits._

import java.io.{ByteArrayOutputStream, PrintStream}
import java.util.UUID

object PagingSelfAwareStructuredLogger {

  /**
   * Wrap a SelfAwareStructuredLogger adding pagination functionality to avoid the truncation (hard splitting) by the
   * underline logging provider. For the functions with a Throwable parameter, the stack trace is appended to the
   * message before pagination.
   *
   * The actual text to be logged has 3 parts:
   *
   * Part 1. The chunk of message in each page;
   * Part 2. This function will add the page header and footer;
   * Part 3. The base class SelfAwareStructuredLogger may add tracing data etc logging context.
   *
   * The total of the above 3 parts should be smaller than the log size limit of the underline logging provider to
   * avoid hard splitting.
   *
   * Example: Assume the log size limit of the underline logging provider is 75 Kb, setting pageSizeK to 64 leaves
   * 11 Kb for (part 2) and (part 3).
   *
   * @param pageSizeK The size (in unit kibibyte) of the chunk of message in each page.
   * @param maxPageNeeded The maximum number of pages to be logged.
   * @param logger The SelfAwareStructuredLogger to be used to do the actual logging.
   * @tparam F Effect type class.
   * @return SelfAwareStructuredLogger with pagination.
   */
  @SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
  def withPaging[F[_]: Monad](pageSizeK: Int = 64, maxPageNeeded: Int = 999)(
      logger: SelfAwareStructuredLogger[F]
  ): SelfAwareStructuredLogger[F] =
    new PagingSelfAwareStructuredLogger[F](pageSizeK, maxPageNeeded)(logger)

  private class PagingSelfAwareStructuredLogger[F[_]: Monad](pageSizeK: Int, maxPageNeeded: Int)(
      sl: SelfAwareStructuredLogger[F]
  ) extends SelfAwareStructuredLogger[F] {
    private val pageIndices = (1 to maxPageNeeded).toList
    private val logSplitIdN = "log_split_id"

    private def pagedLogging(
        loggingOp: (=> String) => F[Unit],
        logSplitId: String,
        msg: => String
    ): F[Unit] = {
      val pageSize = pageSizeK * 1024
      val msgLength = msg.length
      if (msgLength <= pageSize)
        loggingOp(msg)
      else {
        val numOfPages = (msgLength - 1) / pageSize + 1
        val logSplitIdPart1 = logSplitId.split('-').head
        pageIndices
          .take(numOfPages)
          .traverse_ { pi =>
            val pageHeader = s"Page $pi/$numOfPages $logSplitIdPart1"
            val pageFooter =
              s"Page $pi/$numOfPages $logSplitIdN=$logSplitId page_size=${pageSizeK}K"
            val beginIndex = (pi - 1) * pageSize
            val pageContent =
              if (pi < numOfPages)
                msg.substring(beginIndex, beginIndex + pageSize)
              else
                msg.substring(beginIndex)

            loggingOp(show"""$pageHeader
                            |
                            |$pageContent
                            |
                            |$pageFooter""".stripMargin)
          }
      }
    }

    private def addCtx(
        msg: => String,
        ctx: Map[String, String]
    ): (String, Map[String, String]) = {
      val logSplitId = UUID.randomUUID().show
      (
        logSplitId,
        ctx
          .updated(logSplitIdN, logSplitId)
          .updated("log_size", msg.getBytes().length.show)
      )
    }

    @SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
    private def doLogging(
        loggingLevelChk: => F[Boolean],
        logOpWithCtx: Map[String, String] => (=> String) => F[Unit],
        msg: => String,
        ctx: Map[String, String] = Map()
    ): F[Unit] = {
      loggingLevelChk.ifM(
        {
          val (logSplitId, newCtx) = addCtx(msg, ctx)
          pagedLogging(logOpWithCtx(newCtx), logSplitId, msg)
        },
        Applicative[F].unit
      )
    }

    @SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
    private def doLoggingThrowable(
        loggingLevelChk: => F[Boolean],
        logOpWithCtx: Map[String, String] => (=> String) => F[Unit],
        t: Throwable,
        msg: => String,
        ctx: Map[String, String] = Map()
    ): F[Unit] = {
      loggingLevelChk.ifM(
        {
          val stackTrace = printStackTrace(t)
          doLogging(loggingLevelChk, logOpWithCtx, s"$msg\n$stackTrace", ctx)
        },
        Applicative[F].unit
      )
    }

    def printStackTrace(t: Throwable): String = {
      val bos = new ByteArrayOutputStream()
      val ps = new PrintStream(bos)
      t.printStackTrace(ps)
      bos.toString
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
