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
import org.typelevel.log4cats.SelfAwareLogger
import cats.effect.{Ref, Sync}
import cats.syntax.all.*
import org.typelevel.log4cats.extras.LogLevel

import java.util.concurrent.atomic.AtomicReference
import scala.annotation.tailrec

trait TestingLogger[F[_]] extends SelfAwareLogger[F] {
  import TestingLogger.LogMessage
  def logged: F[Vector[LogMessage]]
}

object TestingLogger {

  sealed trait LogMessage {
    def message: String
    def throwOpt: Option[Throwable]
  }

  final case class TRACE(message: String, throwOpt: Option[Throwable]) extends LogMessage
  final case class DEBUG(message: String, throwOpt: Option[Throwable]) extends LogMessage
  final case class INFO(message: String, throwOpt: Option[Throwable]) extends LogMessage
  final case class WARN(message: String, throwOpt: Option[Throwable]) extends LogMessage
  final case class ERROR(message: String, throwOpt: Option[Throwable]) extends LogMessage

  def impl[F[_]: Sync](
      traceEnabled: Boolean = true,
      debugEnabled: Boolean = true,
      infoEnabled: Boolean = true,
      warnEnabled: Boolean = true,
      errorEnabled: Boolean = true
  ): TestingLogger[F] =
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
  ): F[TestingLogger[F]] =
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
  ): TestingLogger[F] = {
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
  ): TestingLogger[F] =
    new TestingLogger[F] {
      def logged: F[Vector[LogMessage]] = read()

      private def shouldLog(ll: LogLevel): Boolean = ll match {
        case LogLevel.Error => errorEnabled
        case LogLevel.Warn => warnEnabled
        case LogLevel.Info => infoEnabled
        case LogLevel.Debug => debugEnabled
        case LogLevel.Trace => traceEnabled
      }

      private def save(ll: LogLevel, t: Option[Throwable], msg: => String): F[Unit] =
        Sync[F].whenA(shouldLog(ll))(appendLogMessage(ll match {
          case LogLevel.Error => ERROR(msg, t)
          case LogLevel.Warn => WARN(msg, t)
          case LogLevel.Info => INFO(msg, t)
          case LogLevel.Debug => DEBUG(msg, t)
          case LogLevel.Trace => TRACE(msg, t)
        }))

      override def isEnabled(ll: LogLevel): F[Boolean] = Sync[F].pure(shouldLog(ll))

      override def log(ll: LogLevel, t: Throwable, msg: => String): F[Unit] = save(ll, t.some, msg)

      override def log(ll: LogLevel, msg: => String): F[Unit] = save(ll, none, msg)
    }

}
