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
abstract class SamLogger[F[_]] extends LoggerKernel[F] {

  final def info(logBit: LogRecord, others: LogRecord*)(implicit
      pkg: sourcecode.Pkg,
      filename: sourcecode.FileName,
      name: sourcecode.Name,
      line: sourcecode.Line
  ): F[Unit] = log_(KernelLogLevel.Info, logBit, others: _*)

  final def warn(logBit: LogRecord, others: LogRecord*)(implicit
      pkg: sourcecode.Pkg,
      filename: sourcecode.FileName,
      name: sourcecode.Name,
      line: sourcecode.Line
  ): F[Unit] = log_(KernelLogLevel.Warn, logBit, others: _*)

  final def error(logBit: LogRecord, others: LogRecord*)(implicit
      pkg: sourcecode.Pkg,
      filename: sourcecode.FileName,
      name: sourcecode.Name,
      line: sourcecode.Line
  ): F[Unit] = log_(KernelLogLevel.Error, logBit, others: _*)

  final def trace(logBit: LogRecord, others: LogRecord*)(implicit
      pkg: sourcecode.Pkg,
      filename: sourcecode.FileName,
      name: sourcecode.Name,
      line: sourcecode.Line
  ): F[Unit] = log_(KernelLogLevel.Trace, logBit, others: _*)

  final def debug(logBit: LogRecord, others: LogRecord*)(implicit
      pkg: sourcecode.Pkg,
      filename: sourcecode.FileName,
      name: sourcecode.Name,
      line: sourcecode.Line
  ): F[Unit] = log_(KernelLogLevel.Debug, logBit, others: _*)

  private final def log_(
      level: KernelLogLevel,
      bit: LogRecord,
      others: LogRecord*
  )(implicit
      pkg: sourcecode.Pkg,
      filename: sourcecode.FileName,
      name: sourcecode.Name,
      line: sourcecode.Line
  ): F[Unit] = {
    log(
      level,
      (record: Log.Builder) =>
        LogRecord.combine(others)(
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

  def withModifiedString(f: String => String): SamLogger[F] =
    SamLogger.withModifiedString[F](this, f)
  def mapK[G[_]](fk: F ~> G): SamLogger[G] = SamLogger.mapK(fk)(this)
}

object SamLogger {
  def apply[F[_]](implicit ev: SamLogger[F]) = ev

  def wrap[F[_]](kernel: LoggerKernel[F]): SamLogger[F] = new SamLogger[F] {
    def log(level: KernelLogLevel, record: Log.Builder => Log.Builder): F[Unit] =
      kernel.log(level, record)
  }

  implicit def optionTSamLogger[F[_]: SamLogger: Functor]: SamLogger[OptionT[F, *]] =
    SamLogger[F].mapK(OptionT.liftK[F])

  implicit def eitherTSamLogger[F[_]: SamLogger: Functor, E]: SamLogger[EitherT[F, E, *]] =
    SamLogger[F].mapK(EitherT.liftK[F, E])

  implicit def kleisliSamLogger[F[_]: SamLogger, A]: SamLogger[Kleisli[F, A, *]] =
    SamLogger[F].mapK(Kleisli.liftK[F, A])

  private def withModifiedString[F[_]](l: SamLogger[F], f: String => String): SamLogger[F] =
    new SamLogger[F] {
      def log(level: KernelLogLevel, record: Log.Builder => Log.Builder): F[Unit] = {
        val modifiedRecord = (builder: Log.Builder) => {
          val originalLog = record(builder).build()
          val modifiedMessage = f(originalLog.message)

          val newBuilder = Log
            .mutableBuilder()
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

  private def mapK[G[_], F[_]](f: G ~> F)(logger: SamLogger[G]): SamLogger[F] =
    new SamLogger[F] {
      def log(level: KernelLogLevel, record: Log.Builder => Log.Builder): F[Unit] =
        f(logger.log(level, record))
    }
}
