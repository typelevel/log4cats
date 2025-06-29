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
import cats.data.{EitherT, Kleisli, OptionT}

/**
 * A SAM-based Logger that extends LoggerKernel and provides a user-friendly interface. This is the
 * new design that will eventually replace the current Logger trait.
 */
abstract class SamLogger[F[_], Ctx] extends LoggerKernel[F, Ctx] {

  final def info(logBit: LogRecord[Ctx], others: LogRecord[Ctx]*)(implicit
      pkg: sourcecode.Pkg,
      filename: sourcecode.FileName,
      name: sourcecode.Name,
      line: sourcecode.Line
  ): F[Unit] = log_(KernelLogLevel.Info, logBit, others: _*)

  final def warn(logBit: LogRecord[Ctx], others: LogRecord[Ctx]*)(implicit
      pkg: sourcecode.Pkg,
      filename: sourcecode.FileName,
      name: sourcecode.Name,
      line: sourcecode.Line
  ): F[Unit] = log_(KernelLogLevel.Warn, logBit, others: _*)

  final def error(logBit: LogRecord[Ctx], others: LogRecord[Ctx]*)(implicit
      pkg: sourcecode.Pkg,
      filename: sourcecode.FileName,
      name: sourcecode.Name,
      line: sourcecode.Line
  ): F[Unit] = log_(KernelLogLevel.Error, logBit, others: _*)

  final def trace(logBit: LogRecord[Ctx], others: LogRecord[Ctx]*)(implicit
      pkg: sourcecode.Pkg,
      filename: sourcecode.FileName,
      name: sourcecode.Name,
      line: sourcecode.Line
  ): F[Unit] = log_(KernelLogLevel.Trace, logBit, others: _*)

  final def debug(logBit: LogRecord[Ctx], others: LogRecord[Ctx]*)(implicit
      pkg: sourcecode.Pkg,
      filename: sourcecode.FileName,
      name: sourcecode.Name,
      line: sourcecode.Line
  ): F[Unit] = log_(KernelLogLevel.Debug, logBit, others: _*)

  private final def log_(
      level: KernelLogLevel,
      bit: LogRecord[Ctx],
      others: LogRecord[Ctx]*
  )(implicit
      pkg: sourcecode.Pkg,
      filename: sourcecode.FileName,
      name: sourcecode.Name,
      line: sourcecode.Line
  ): F[Unit] = {
    log(
      level,
      (record: Builder) =>
        LogRecord.combine[Ctx](others)(
          bit(
            record
              .withLevel(level)
              .withClassName(pkg.value + "." + name.value)
              .withFileName(filename.value)
              .withLine(line.value)
          )
        )
    )
  }

  def withModifiedString(f: String => String): SamLogger[F, Ctx] =
    SamLogger.withModifiedString[F, Ctx](this, f)
  def mapK[G[_]](fk: F ~> G): SamLogger[G, Ctx] = SamLogger.mapK(fk)(this)
}

object SamLogger {
  def apply[F[_], Ctx](implicit ev: SamLogger[F, Ctx]) = ev

  def wrap[F[_], Ctx](kernel: LoggerKernel[F, Ctx]): SamLogger[F, Ctx] = new SamLogger[F, Ctx] {
    def log(level: KernelLogLevel, record: Builder => Builder): F[Unit] =
      kernel.log(level, record)
  }

  implicit def optionTSamLogger[F[_]: Functor, Ctx](implicit
      ev: SamLogger[F, Ctx]
  ): SamLogger[OptionT[F, *], Ctx] =
    ev.mapK(OptionT.liftK[F])

  implicit def eitherTSamLogger[F[_]: Functor, E, Ctx](implicit
      ev: SamLogger[F, Ctx]
  ): SamLogger[EitherT[F, E, *], Ctx] =
    ev.mapK(EitherT.liftK[F, E])

  implicit def kleisliSamLogger[F[_], A, Ctx](implicit
      ev: SamLogger[F, Ctx]
  ): SamLogger[Kleisli[F, A, *], Ctx] =
    ev.mapK(Kleisli.liftK[F, A])

  private def withModifiedString[F[_], Ctx](
      l: SamLogger[F, Ctx],
      f: String => String
  ): SamLogger[F, Ctx] =
    new SamLogger[F, Ctx] {
      def log(level: KernelLogLevel, record: Builder => Builder): F[Unit] = {
        val modifiedRecord = (builder: Builder) => {
          val originalLog = record(builder).build()
          val modifiedMessage = f(originalLog.message)

          val newBuilder = Log
            .mutableBuilder[Ctx]()
            .withLevel(level)
            .withMessage(modifiedMessage)

          // we can copy the fields from the original log like this
          originalLog.timestamp.foreach(newBuilder.withTimestamp)
          originalLog.throwable.foreach(newBuilder.withThrowable)

          originalLog.fileName.foreach(newBuilder.withFileName)
          originalLog.className.foreach(newBuilder.withClassName)
          originalLog.line.foreach(newBuilder.withLine)

          originalLog.context.foreach { case (k, v) =>
            newBuilder.withContext(k)(v)
          }

          newBuilder
        }
        l.log(level, modifiedRecord)
      }
    }

  private def mapK[G[_], F[_], Ctx](f: G ~> F)(logger: SamLogger[G, Ctx]): SamLogger[F, Ctx] =
    new SamLogger[F, Ctx] {
      def log(level: KernelLogLevel, record: Builder => Builder): F[Unit] =
        f(logger.log(level, record))
    }
}
