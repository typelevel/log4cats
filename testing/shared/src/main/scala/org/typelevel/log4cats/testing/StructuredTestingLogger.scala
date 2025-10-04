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

package org.typelevel.log4cats.testing

import cats.data.Chain
import org.typelevel.log4cats.{KernelLogLevel, LoggerKernel, SelfAwareStructuredLogger}
import org.typelevel.log4cats.Log
import cats.effect.{Ref, Sync}
import cats.syntax.all.*

import java.util.concurrent.atomic.AtomicReference
import scala.annotation.tailrec

trait StructuredTestingLogger[F[_]] extends SelfAwareStructuredLogger[F] {
  import StructuredTestingLogger.LogMessage
  def logged: F[Vector[LogMessage]]
}

object StructuredTestingLogger {

  sealed trait LogMessage {
    def ctx: Map[String, String]
    def message: String
    def throwOpt: Option[Throwable]
  }

  final case class TRACE(
      message: String,
      throwOpt: Option[Throwable],
      ctx: Map[String, String] = Map.empty
  ) extends LogMessage
  final case class DEBUG(
      message: String,
      throwOpt: Option[Throwable],
      ctx: Map[String, String] = Map.empty
  ) extends LogMessage
  final case class INFO(
      message: String,
      throwOpt: Option[Throwable],
      ctx: Map[String, String] = Map.empty
  ) extends LogMessage
  final case class WARN(
      message: String,
      throwOpt: Option[Throwable],
      ctx: Map[String, String] = Map.empty
  ) extends LogMessage
  final case class ERROR(
      message: String,
      throwOpt: Option[Throwable],
      ctx: Map[String, String] = Map.empty
  ) extends LogMessage

  def impl[F[_]: Sync](
      traceEnabled: Boolean = true,
      debugEnabled: Boolean = true,
      infoEnabled: Boolean = true,
      warnEnabled: Boolean = true,
      errorEnabled: Boolean = true
  ): StructuredTestingLogger[F] =
    atomic[F](
      traceEnabled = traceEnabled,
      debugEnabled = debugEnabled,
      infoEnabled = infoEnabled,
      warnEnabled = warnEnabled,
      errorEnabled = errorEnabled
    )

  def ref[F[_]: Sync](
      traceEnabled: Boolean = true,
      debugEnabled: Boolean = true,
      infoEnabled: Boolean = true,
      warnEnabled: Boolean = true,
      errorEnabled: Boolean = true
  ): F[StructuredTestingLogger[F]] =
    Ref[F].empty[Chain[LogMessage]].map { ref =>
      make[F](
        traceEnabled = traceEnabled,
        debugEnabled = debugEnabled,
        infoEnabled = infoEnabled,
        warnEnabled = warnEnabled,
        errorEnabled = errorEnabled,
        appendLogMessage = lm => ref.update(_.append(lm)),
        read = () => ref.get.map(_.toVector)
      )
    }

  def atomic[F[_]: Sync](
      traceEnabled: Boolean = true,
      debugEnabled: Boolean = true,
      infoEnabled: Boolean = true,
      warnEnabled: Boolean = true,
      errorEnabled: Boolean = true
  ): StructuredTestingLogger[F] = {
    val ar = new AtomicReference(Vector.empty[LogMessage])
    def appendLogMessage(m: LogMessage): F[Unit] = Sync[F].delay {
      @tailrec
      def mod(): Unit = {
        val c = ar.get
        val u = c :+ m
        if (!ar.compareAndSet(c, u)) mod()
        else ()
      }
      mod()
    }

    make[F](
      traceEnabled = traceEnabled,
      debugEnabled = debugEnabled,
      infoEnabled = infoEnabled,
      warnEnabled = warnEnabled,
      errorEnabled = errorEnabled,
      appendLogMessage = appendLogMessage,
      read = () => Sync[F].delay(ar.get())
    )
  }

  def make[F[_]: Sync](
      traceEnabled: Boolean = true,
      debugEnabled: Boolean = true,
      infoEnabled: Boolean = true,
      warnEnabled: Boolean = true,
      errorEnabled: Boolean = true,
      appendLogMessage: LogMessage => F[Unit],
      read: () => F[Vector[LogMessage]]
  ): StructuredTestingLogger[F] =
    new StructuredTestingLogger[F] {
      def logged: F[Vector[LogMessage]] = read()

      def isTraceEnabled: F[Boolean] = Sync[F].pure(traceEnabled)
      def isDebugEnabled: F[Boolean] = Sync[F].pure(debugEnabled)
      def isInfoEnabled: F[Boolean] = Sync[F].pure(infoEnabled)
      def isWarnEnabled: F[Boolean] = Sync[F].pure(warnEnabled)
      def isErrorEnabled: F[Boolean] = Sync[F].pure(errorEnabled)

      protected def kernel: LoggerKernel[F, String] = new LoggerKernel[F, String] {
        def log(
            level: KernelLogLevel,
            logBuilder: Log.Builder[String] => Log.Builder[String]
        ): F[Unit] = {
          val log = logBuilder(Log.mutableBuilder[String]()).build()
          val message = log.message()
          val throwable = log.throwable
          val context = log.context

          level match {
            case KernelLogLevel.Trace =>
              if (traceEnabled) appendLogMessage(TRACE(message, throwable, context))
              else Sync[F].unit
            case KernelLogLevel.Debug =>
              if (debugEnabled) appendLogMessage(DEBUG(message, throwable, context))
              else Sync[F].unit
            case KernelLogLevel.Info =>
              if (infoEnabled) appendLogMessage(INFO(message, throwable, context)) else Sync[F].unit
            case KernelLogLevel.Warn =>
              if (warnEnabled) appendLogMessage(WARN(message, throwable, context)) else Sync[F].unit
            case KernelLogLevel.Error =>
              if (errorEnabled) appendLogMessage(ERROR(message, throwable, context))
              else Sync[F].unit
          }
        }
      }

      private val noop = Sync[F].unit

      override def error(message: => String): F[Unit] =
        if (errorEnabled) appendLogMessage(ERROR(message, None)) else noop
      override def error(t: Throwable)(message: => String): F[Unit] =
        if (errorEnabled) appendLogMessage(ERROR(message, t.some)) else noop
      override def error(ctx: Map[String, String])(message: => String): F[Unit] =
        if (errorEnabled) appendLogMessage(ERROR(message, None, ctx)) else noop
      override def error(ctx: Map[String, String], t: Throwable)(message: => String): F[Unit] =
        if (errorEnabled) appendLogMessage(ERROR(message, t.some, ctx)) else noop

      override def warn(message: => String): F[Unit] =
        if (warnEnabled) appendLogMessage(WARN(message, None)) else noop
      override def warn(t: Throwable)(message: => String): F[Unit] =
        if (warnEnabled) appendLogMessage(WARN(message, t.some)) else noop
      override def warn(ctx: Map[String, String])(message: => String): F[Unit] =
        if (warnEnabled) appendLogMessage(WARN(message, None, ctx)) else noop
      override def warn(ctx: Map[String, String], t: Throwable)(message: => String): F[Unit] =
        if (warnEnabled) appendLogMessage(WARN(message, t.some, ctx)) else noop

      override def info(message: => String): F[Unit] =
        if (infoEnabled) appendLogMessage(INFO(message, None)) else noop
      override def info(t: Throwable)(message: => String): F[Unit] =
        if (infoEnabled) appendLogMessage(INFO(message, t.some)) else noop
      override def info(ctx: Map[String, String])(message: => String): F[Unit] =
        if (infoEnabled) appendLogMessage(INFO(message, None, ctx)) else noop
      override def info(ctx: Map[String, String], t: Throwable)(message: => String): F[Unit] =
        if (infoEnabled) appendLogMessage(INFO(message, t.some, ctx)) else noop

      override def debug(message: => String): F[Unit] =
        if (debugEnabled) appendLogMessage(DEBUG(message, None)) else noop
      override def debug(t: Throwable)(message: => String): F[Unit] =
        if (debugEnabled) appendLogMessage(DEBUG(message, t.some)) else noop
      override def debug(ctx: Map[String, String])(message: => String): F[Unit] =
        if (debugEnabled) appendLogMessage(DEBUG(message, None, ctx)) else noop
      override def debug(ctx: Map[String, String], t: Throwable)(message: => String): F[Unit] =
        if (debugEnabled) appendLogMessage(DEBUG(message, t.some, ctx)) else noop

      override def trace(message: => String): F[Unit] =
        if (traceEnabled) appendLogMessage(TRACE(message, None)) else noop
      override def trace(t: Throwable)(message: => String): F[Unit] =
        if (traceEnabled) appendLogMessage(TRACE(message, t.some)) else noop
      override def trace(ctx: Map[String, String])(message: => String): F[Unit] =
        if (traceEnabled) appendLogMessage(TRACE(message, None, ctx)) else noop
      override def trace(ctx: Map[String, String], t: Throwable)(message: => String): F[Unit] =
        if (traceEnabled) appendLogMessage(TRACE(message, t.some, ctx)) else noop
    }
}
