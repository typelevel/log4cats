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

import cats.syntax.show.*
import cats.Show
import cats.kernel.Hash
import org.typelevel.log4cats.{Logger, StructuredLogger}
import org.typelevel.log4cats.extras.DeferredLogMessage.{
  deferredStructuredLogMessageHash,
  deferredStructuredLogMessageShow
}

/**
 * `StructuredLogMessage` has a bug that can't be fixed without breaking bincompat (because it's a
 * `case class`), but it's only used in the `Writer*Logger`s, so it's not a huge deal.
 *
 * The issue is that the API of the `*Logger` classes has a by-name parameter for the message, and
 * `StructuredLogMessage` (and by extension, the `Writer*Logger`) don't lazily compute the message.
 *
 * At some point, this should be renamed to `StructuredLogMessage` and replace the old class.
 */
sealed trait DeferredLogMessage {
  def level: DefferedLogLevel
  def context: Map[String, String]
  def throwableOpt: Option[Throwable]
  def message: () => String

  def log[F[_]](logger: Logger[F]): F[Unit] = {
    level match {
      case DefferedLogLevel.Error =>
        throwableOpt match {
          case Some(e) => logger.error(e)(message())
          case None => logger.error(message())
        }
      case DefferedLogLevel.Warn =>
        throwableOpt match {
          case Some(e) => logger.warn(e)(message())
          case None => logger.warn(message())
        }
      case DefferedLogLevel.Info =>
        throwableOpt match {
          case Some(e) => logger.info(e)(message())
          case None => logger.info(message())
        }
      case DefferedLogLevel.Debug =>
        throwableOpt match {
          case Some(e) => logger.debug(e)(message())
          case None => logger.debug(message())
        }
      case DefferedLogLevel.Trace =>
        throwableOpt match {
          case Some(e) => logger.trace(e)(message())
          case None => logger.trace(message())
        }
    }
  }

  def logStructured[F[_]](logger: StructuredLogger[F]): F[Unit] = {
    level match {
      case DefferedLogLevel.Error =>
        throwableOpt match {
          case Some(e) => logger.error(context, e)(message())
          case None => logger.error(context)(message())
        }
      case DefferedLogLevel.Warn =>
        throwableOpt match {
          case Some(e) => logger.warn(context, e)(message())
          case None => logger.warn(context)(message())
        }
      case DefferedLogLevel.Info =>
        throwableOpt match {
          case Some(e) => logger.info(context, e)(message())
          case None => logger.info(context)(message())
        }
      case DefferedLogLevel.Debug =>
        throwableOpt match {
          case Some(e) => logger.debug(context, e)(message())
          case None => logger.debug(context)(message())
        }
      case DefferedLogLevel.Trace =>
        throwableOpt match {
          case Some(e) => logger.trace(context, e)(message())
          case None => logger.trace(context)(message())
        }
    }
  }

  override def equals(obj: Any): Boolean = obj match {
    case other: DeferredLogMessage => deferredStructuredLogMessageHash.eqv(this, other)
    case _ => false
  }

  override def hashCode(): Int = deferredStructuredLogMessageHash.hash(this)

  override def toString: String = deferredStructuredLogMessageShow.show(this)
}
object DeferredLogMessage {
  def apply(
      l: DefferedLogLevel,
      c: Map[String, String],
      t: Option[Throwable],
      m: () => String
  ): DeferredLogMessage =
    new DeferredLogMessage {
      override val level: DefferedLogLevel = l
      override val context: Map[String, String] = c
      override val throwableOpt: Option[Throwable] = t
      override val message: () => String = m
    }

  def trace(c: Map[String, String], t: Option[Throwable], m: () => String): DeferredLogMessage =
    apply(DefferedLogLevel.Trace, c, t, m)

  def debug(c: Map[String, String], t: Option[Throwable], m: () => String): DeferredLogMessage =
    apply(DefferedLogLevel.Debug, c, t, m)

  def info(c: Map[String, String], t: Option[Throwable], m: () => String): DeferredLogMessage =
    apply(DefferedLogLevel.Info, c, t, m)

  def warn(c: Map[String, String], t: Option[Throwable], m: () => String): DeferredLogMessage =
    apply(DefferedLogLevel.Warn, c, t, m)

  def error(c: Map[String, String], t: Option[Throwable], m: () => String): DeferredLogMessage =
    apply(DefferedLogLevel.Error, c, t, m)

  implicit val deferredStructuredLogMessageHash: Hash[DeferredLogMessage] = Hash.by { l =>
    (l.level, l.context, l.throwableOpt.map(_.getMessage), l.message())
  }

  implicit val deferredStructuredLogMessageShow: Show[DeferredLogMessage] = Show.show { l =>
    show"DeferredStructuredLogMessage(${l.level},${l.context},${l.throwableOpt.map(_.getMessage)},${l.message()})"
  }
}
