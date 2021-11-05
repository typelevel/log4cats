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

private class PagingSelfAwareStructuredLogger[F[_]: Monad](pageSize: Int)(
    sl: SelfAwareStructuredLogger[F]
) extends SelfAwareStructuredLogger[F] {
  private def paging(
      loggingLevelChk: => F[Boolean],
      loggingOp: String => F[Unit],
      msg: => String
  ): F[Unit] = {
    loggingLevelChk.ifM(
      {
        val length = msg.getBytes().length
        val decorationSize =
          Math.max(
            Math.min(pageSize / 10, 2048),
            512
          )
        val pageContentSize = pageSize - decorationSize
        if (length <= pageContentSize)
          loggingOp(msg)
        else {
          val pl = msg.grouped(pageContentSize).toList.zip(LazyList from 1)
          val numOfPages = pl.length
          pl.traverse(msgIndexTuple =>
            loggingOp(
              show"""
                  |Page ${msgIndexTuple._2} / $numOfPages
                  |${msgIndexTuple._1}""".stripMargin
            )
          )
        }
      },
      Applicative[F].unit
    )
  }

  override def trace(ctx: Map[String, String])(msg: => String): F[Unit] =
    paging(isTraceEnabled, sl.trace(ctx), msg)

  override def trace(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
    paging(isTraceEnabled, sl.trace(ctx, t), msg)

  override def debug(ctx: Map[String, String])(msg: => String): F[Unit] =
    paging(isDebugEnabled, sl.debug(ctx), msg)

  override def debug(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
    paging(isDebugEnabled, sl.debug(ctx, t), msg)

  override def info(ctx: Map[String, String])(msg: => String): F[Unit] =
    paging(isInfoEnabled, sl.info(ctx), msg)

  override def info(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
    paging(isInfoEnabled, sl.info(ctx, t), msg)

  override def warn(ctx: Map[String, String])(msg: => String): F[Unit] =
    paging(isWarnEnabled, sl.warn(ctx), msg)

  override def warn(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
    paging(isWarnEnabled, sl.warn(ctx, t), msg)

  override def error(ctx: Map[String, String])(msg: => String): F[Unit] =
    paging(isErrorEnabled, sl.error(ctx), msg)

  override def error(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
    paging(isErrorEnabled, sl.error(ctx, t), msg)

  override def isTraceEnabled: F[Boolean] = sl.isTraceEnabled

  override def isDebugEnabled: F[Boolean] = sl.isDebugEnabled

  override def isInfoEnabled: F[Boolean] = sl.isInfoEnabled

  override def isWarnEnabled: F[Boolean] = sl.isWarnEnabled

  override def isErrorEnabled: F[Boolean] = sl.isErrorEnabled

  override def error(message: => String): F[Unit] =
    paging(isErrorEnabled, sl.error, message)

  override def warn(message: => String): F[Unit] =
    paging(isWarnEnabled, sl.warn, message)

  override def info(message: => String): F[Unit] =
    paging(isInfoEnabled, sl.info, message)

  override def debug(message: => String): F[Unit] =
    paging(isDebugEnabled, sl.debug, message)

  override def trace(message: => String): F[Unit] =
    paging(isTraceEnabled, sl.trace, message)

  override def error(t: Throwable)(message: => String): F[Unit] =
    paging(isErrorEnabled, sl.error(t), message)

  override def warn(t: Throwable)(message: => String): F[Unit] =
    paging(isWarnEnabled, sl.warn(t), message)

  override def info(t: Throwable)(message: => String): F[Unit] =
    paging(isInfoEnabled, sl.info(t), message)

  override def debug(t: Throwable)(message: => String): F[Unit] =
    paging(isDebugEnabled, sl.debug(t), message)

  override def trace(t: Throwable)(message: => String): F[Unit] =
    paging(isTraceEnabled, sl.trace(t), message)
}
