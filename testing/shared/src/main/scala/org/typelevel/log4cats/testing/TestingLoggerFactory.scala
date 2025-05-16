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

import cats.Show
import cats.data.Chain
import cats.effect.{Ref, Sync}
import cats.syntax.all.*
import org.typelevel.log4cats.extras.LogLevel
import org.typelevel.log4cats.testing.TestingLoggerFactory.LogMessage
import org.typelevel.log4cats.{LoggerFactory, SelfAwareStructuredLogger}

import java.io.{PrintWriter, StringWriter}
import java.util.concurrent.atomic.AtomicReference
import scala.annotation.tailrec

trait TestingLoggerFactory[F[_]] extends LoggerFactory[F] {
  def logged: F[Vector[LogMessage]]
}
object TestingLoggerFactory {

  sealed trait LogMessage {
    def loggerName: String
    def ctx: Map[String, String]
    def message: String
    def throwOpt: Option[Throwable]
    def level: LogLevel = this match {
      case _: Trace => LogLevel.Trace
      case _: Debug => LogLevel.Debug
      case _: Info => LogLevel.Info
      case _: Warn => LogLevel.Warn
      case _: Error => LogLevel.Error
    }
  }

  final case class Trace(
      loggerName: String,
      message: String,
      throwOpt: Option[Throwable],
      ctx: Map[String, String] = Map.empty
  ) extends LogMessage
  final case class Debug(
      loggerName: String,
      message: String,
      throwOpt: Option[Throwable],
      ctx: Map[String, String] = Map.empty
  ) extends LogMessage
  final case class Info(
      loggerName: String,
      message: String,
      throwOpt: Option[Throwable],
      ctx: Map[String, String] = Map.empty
  ) extends LogMessage
  final case class Warn(
      loggerName: String,
      message: String,
      throwOpt: Option[Throwable],
      ctx: Map[String, String] = Map.empty
  ) extends LogMessage
  final case class Error(
      loggerName: String,
      message: String,
      throwOpt: Option[Throwable],
      ctx: Map[String, String] = Map.empty
  ) extends LogMessage

  implicit val showMsg: Show[LogMessage] = Show.show { log =>
    val builder = new StringBuilder()
    builder
      .append(log.loggerName)
      .append(" [")
      .append(log match {
        case _: Trace => "TRACE"
        case _: Debug => "DEBUG"
        case _: Info => "INFO"
        case _: Warn => "WARN"
        case _: Error => "ERROR"
      })
      .append("] ")
      .append(log.message)
    log.throwOpt.foreach { t =>
      builder.append(" ")
      val sw = new StringWriter()
      val pw = new PrintWriter(sw)
      t.printStackTrace(pw)
      builder.append(sw)
      pw.close()
    }
    if (log.ctx.nonEmpty) {
      builder.append('\n')
    }
    log.ctx.foreach { case (k, v) =>
      builder.append("   ").append(k).append(':').append(v).append('\n')
    }
    builder.result()
  }

  def ref[F[_]: Sync](
      traceEnabled: Boolean = true,
      debugEnabled: Boolean = true,
      infoEnabled: Boolean = true,
      warnEnabled: Boolean = true,
      errorEnabled: Boolean = true
  ): F[TestingLoggerFactory[F]] =
    Ref[F].empty[Chain[LogMessage]].map { ref =>
      make[F](
        traceEnabled = traceEnabled,
        debugEnabled = debugEnabled,
        infoEnabled = infoEnabled,
        warnEnabled = warnEnabled,
        errorEnabled = errorEnabled,
        save = lm => ref.update(_.append(lm)),
        read = () => ref.get.map(_.toVector)
      )
    }

  def atomic[F[_]: Sync](
      traceEnabled: Boolean = true,
      debugEnabled: Boolean = true,
      infoEnabled: Boolean = true,
      warnEnabled: Boolean = true,
      errorEnabled: Boolean = true
  ): TestingLoggerFactory[F] = {
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
      save = appendLogMessage,
      read = () => Sync[F].delay(ar.get())
    )
  }

  def make[F[_]: Sync](
      traceEnabled: Boolean = true,
      debugEnabled: Boolean = true,
      infoEnabled: Boolean = true,
      warnEnabled: Boolean = true,
      errorEnabled: Boolean = true,
      save: LogMessage => F[Unit],
      read: () => F[Vector[LogMessage]]
  ): TestingLoggerFactory[F] =
    new TestingLoggerFactory[F] {
      override def logged: F[Vector[LogMessage]] = read()
      override def fromName(name: String): F[SelfAwareStructuredLogger[F]] =
        Sync[F].delay(getLoggerFromName(name))
      override def getLoggerFromName(name: String): SelfAwareStructuredLogger[F] =
        new SelfAwareStructuredLogger[F] {

          private def shouldLog(ll: LogLevel): Boolean = ll match {
            case LogLevel.Error => errorEnabled
            case LogLevel.Warn => warnEnabled
            case LogLevel.Info => infoEnabled
            case LogLevel.Debug => debugEnabled
            case LogLevel.Trace => traceEnabled
          }

          private def write(
              ll: LogLevel,
              ctx: Map[String, String],
              t: Option[Throwable],
              msg: => String
          ): F[Unit] =
            Sync[F].whenA(shouldLog(ll))(save(ll match {
              case LogLevel.Error => Error(name, msg, t, ctx)
              case LogLevel.Warn => Warn(name, msg, t, ctx)
              case LogLevel.Info => Info(name, msg, t, ctx)
              case LogLevel.Debug => Debug(name, msg, t, ctx)
              case LogLevel.Trace => Trace(name, msg, t, ctx)
            }))

          override def isEnabled(ll: LogLevel): F[Boolean] = Sync[F].pure(shouldLog(ll))

          override def log(
              ll: LogLevel,
              ctx: Map[String, String],
              t: Throwable,
              msg: => String
          ): F[Unit] =
            write(ll, ctx, t.some, msg)

          override def log(ll: LogLevel, ctx: Map[String, String], msg: => String): F[Unit] =
            write(ll, ctx, none, msg)

          override def log(ll: LogLevel, t: Throwable, msg: => String): F[Unit] =
            write(ll, Map.empty, t.some, msg)

          override def log(ll: LogLevel, msg: => String): F[Unit] = write(ll, Map.empty, none, msg)
        }

    }
}
