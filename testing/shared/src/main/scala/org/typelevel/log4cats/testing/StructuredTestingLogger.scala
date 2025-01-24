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
import org.typelevel.log4cats.SelfAwareStructuredLogger
import cats.effect.{Ref, Sync}
import cats.syntax.all.*
import org.typelevel.log4cats.extras.LogLevel

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

      private def shouldLog(ll: LogLevel): Boolean = ll match {
        case LogLevel.Error => errorEnabled
        case LogLevel.Warn => warnEnabled
        case LogLevel.Info => infoEnabled
        case LogLevel.Debug => debugEnabled
        case LogLevel.Trace => traceEnabled
      }

      private def save(
          ll: LogLevel,
          ctx: Map[String, String],
          t: Option[Throwable],
          msg: => String
      ): F[Unit] =
        Sync[F].whenA(shouldLog(ll))(appendLogMessage(ll match {
          case LogLevel.Error => ERROR(msg, t, ctx)
          case LogLevel.Warn => WARN(msg, t, ctx)
          case LogLevel.Info => INFO(msg, t, ctx)
          case LogLevel.Debug => DEBUG(msg, t, ctx)
          case LogLevel.Trace => TRACE(msg, t, ctx)
        }))

      override def isEnabled(ll: LogLevel): F[Boolean] = Sync[F].pure(shouldLog(ll))

      override def log(
          ll: LogLevel,
          ctx: Map[String, String],
          t: Throwable,
          msg: => String
      ): F[Unit] = save(ll, ctx, t.some, msg)

      override def log(ll: LogLevel, ctx: Map[String, String], msg: => String): F[Unit] =
        save(ll, ctx, none, msg)

      override def log(ll: LogLevel, t: Throwable, msg: => String): F[Unit] =
        save(ll, Map.empty, t.some, msg)

      override def log(ll: LogLevel, msg: => String): F[Unit] = save(ll, Map.empty, none, msg)
    }
}
