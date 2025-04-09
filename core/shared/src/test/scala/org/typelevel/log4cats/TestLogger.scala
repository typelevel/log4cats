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

import cats.{Functor, Monad}
import cats.effect.unsafe.IORuntime
import cats.effect.{IO, Ref}
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.order._
import org.typelevel.log4cats.extras.LogLevel

import scala.collection.immutable.Queue

// `finestLogLevel` of `None` means all logging is disabled`
final class TestLogger[F[_]] private (
    val output: TestLogger.Output[F],
    name: String,
    finestLogLevel: Ref[F, Option[LogLevel]]
)(implicit F: Monad[F])
    extends SelfAwareStructuredLogger[F] {

  /** Enables logging with the finest level enabled set to the given level. */
  def enableLoggingWithFinestLevel(level: LogLevel): F[Unit] =
    finestLogLevel.set(Some(level))

  /**
   * Disables all logging.
   *
   * Does NOT reset logged entries.
   */
  def disableLogging: F[Unit] =
    finestLogLevel.set(None)

  private[this] def isLevelEnabled(level: LogLevel): F[Boolean] =
    finestLogLevel.get.map(_.exists(level >= _))

  def isErrorEnabled: F[Boolean] = isLevelEnabled(LogLevel.Error)
  def isWarnEnabled: F[Boolean] = isLevelEnabled(LogLevel.Warn)
  def isInfoEnabled: F[Boolean] = isLevelEnabled(LogLevel.Info)
  def isDebugEnabled: F[Boolean] = isLevelEnabled(LogLevel.Debug)
  def isTraceEnabled: F[Boolean] = isLevelEnabled(LogLevel.Trace)

  protected[this] def doLog(
      level: LogLevel,
      exception: Option[Throwable],
      ctx: Map[String, String]
  )(msg: => String): F[Unit] =
    F.ifM(isLevelEnabled(level))(
      output.append(
        TestLogger.Entry(
          loggerName = name,
          level = level,
          message = msg,
          exception = exception,
          context = ctx
        )
      ),
      F.unit
    )

  def error(message: => String): F[Unit] =
    doLog(LogLevel.Error, None, Map.empty)(message)
  def error(t: Throwable)(message: => String): F[Unit] =
    doLog(LogLevel.Error, Some(t), Map.empty)(message)
  def error(ctx: Map[String, String])(msg: => String): F[Unit] =
    doLog(LogLevel.Error, None, ctx)(msg)
  def error(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
    doLog(LogLevel.Error, Some(t), ctx)(msg)

  def warn(message: => String): F[Unit] =
    doLog(LogLevel.Warn, None, Map.empty)(message)
  def warn(t: Throwable)(message: => String): F[Unit] =
    doLog(LogLevel.Warn, Some(t), Map.empty)(message)
  def warn(ctx: Map[String, String])(msg: => String): F[Unit] =
    doLog(LogLevel.Warn, None, ctx)(msg)
  def warn(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
    doLog(LogLevel.Warn, Some(t), ctx)(msg)

  def info(message: => String): F[Unit] =
    doLog(LogLevel.Info, None, Map.empty)(message)
  def info(t: Throwable)(message: => String): F[Unit] =
    doLog(LogLevel.Info, Some(t), Map.empty)(message)
  def info(ctx: Map[String, String])(msg: => String): F[Unit] =
    doLog(LogLevel.Info, None, ctx)(msg)
  def info(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
    doLog(LogLevel.Info, Some(t), ctx)(msg)

  def debug(message: => String): F[Unit] =
    doLog(LogLevel.Debug, None, Map.empty)(message)
  def debug(t: Throwable)(message: => String): F[Unit] =
    doLog(LogLevel.Debug, Some(t), Map.empty)(message)
  def debug(ctx: Map[String, String])(msg: => String): F[Unit] =
    doLog(LogLevel.Debug, None, ctx)(msg)
  def debug(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
    doLog(LogLevel.Debug, Some(t), ctx)(msg)

  def trace(message: => String): F[Unit] =
    doLog(LogLevel.Trace, None, Map.empty)(message)
  def trace(t: Throwable)(message: => String): F[Unit] =
    doLog(LogLevel.Trace, Some(t), Map.empty)(message)
  def trace(ctx: Map[String, String])(msg: => String): F[Unit] =
    doLog(LogLevel.Trace, None, ctx)(msg)
  def trace(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
    doLog(LogLevel.Trace, Some(t), ctx)(msg)
}

object TestLogger {
  final case class Entry(
      loggerName: String,
      level: LogLevel,
      message: String,
      exception: Option[Throwable],
      context: Map[String, String]
  )

  /** Output from logging. */
  final class Output[F[_]: Functor] private (logEntries: Ref[F, Queue[TestLogger.Entry]]) {

    private[TestLogger] def append(entry: Entry): F[Unit] =
      logEntries.update(_ :+ entry)

    /** @return entries that have been logged */
    def loggedEntries: F[Seq[TestLogger.Entry]] =
      logEntries.get.widen

    /** Removes all logged entries. */
    def resetLoggedEntries: F[Unit] =
      logEntries.set(Queue.empty)
  }

  object Output {
    def apply[F[_]: Functor: Ref.Make]: F[Output[F]] =
      Ref.of(Queue.empty[Entry]).map(new Output(_))
  }

  final class Factory private (
      val output: Output[IO]
  )(implicit runtime: IORuntime)
      extends LoggerFactory[IO] {

    // these should probably return the same instance for the same name, but
    // they have no documentation or specification, and it's much easier for
    // them not to
    def fromName(name: String): IO[TestLogger[IO]] =
      IO.ref(Some(LogLevel.Trace): Option[LogLevel])
        .map(new TestLogger(output, name, _))
    def getLoggerFromName(name: String): TestLogger[IO] =
      fromName(name).unsafeRunSync()
  }

  object Factory {
    def apply()(implicit runtime: IORuntime): IO[Factory] =
      Output[IO].map(new Factory(_))
  }

  def apply[F[_]: Monad: Ref.Make](
      name: String,
      finestLevelEnabled: Option[LogLevel] = Some(LogLevel.Trace)
  ): F[TestLogger[F]] =
    for {
      output <- Output[F]
      finestLogLevel <- Ref.of(finestLevelEnabled)
    } yield new TestLogger(output, name, finestLogLevel)
}
