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

import cats.Show.Shown
import cats.data.Chain
import cats.effect.kernel.{Async, Ref, Resource}
import cats.effect.std.{Console, Dispatcher}
import cats.syntax.all.*
import cats.{~>, Functor}
import org.typelevel.log4cats.{LoggerFactory, SelfAwareStructuredLogger}

/**
 * Creates simple loggers that print logs to standard error output.
 *
 * The intended use-case is for one-off scripts, examples, and other situations where a more
 * fully-featured and performance-optimized logger would be overkill
 */
trait ConsoleLoggerFactory[F[_]] extends LoggerFactory[F] {
  override def getLoggerFromName(name: String): SelfAwareStructuredLogger[F]

  /**
   * Set the initial log level for loggers that will be created.
   *
   * @param logLevel
   *   Sets the log level such that logs at or above this level will be logged.
   * @note
   *   This does not propagate to loggers which have already been created.
   */
  def setGlobalLogLevel(logLevel: LogLevel): F[Unit]

  /**
   * Add an override for loggers that will be created which have a matching prefix.
   *
   * @param prefix
   *   Similar to how `name` works in a `log4j` config file, this is a simple prefix match.
   * @note
   *   This does not propagate to loggers which have already been created.
   * @note
   *   These are applied on a first-match basis:
   *   {{{
   *   logger.addLogLevelOverride("", LogLevel.Warn) *>
   *   logger.addLogLevelOverride("foo", LogLevel.Info) // This is effectively dead code
   *   }}}
   */
  def addLogLevelOverride(prefix: String, logLevel: LogLevel): F[Unit]

  /**
   * Remove an existing log level override.
   *
   * @param prefix
   *   This must match exactly, if overrides for `foo.bar` and `foo` have been added, then removing
   *   `foo` will not remove the override for `foo.bar`
   * @note
   *   This does not propagate to loggers which have already been created.
   */
  def removeLogLevelOverride(prefix: String): F[Unit]

  override def addContext(ctx: Map[String, String])(implicit
      F: Functor[F]
  ): ConsoleLoggerFactory[F] =
    ConsoleLoggerFactory.addContext(this, ctx)

  override def addContext(pairs: (String, Shown)*)(implicit
      F: Functor[F]
  ): ConsoleLoggerFactory[F] =
    ConsoleLoggerFactory.addContext(this, pairs.map { case (k, v) => (k, v.toString) }.toMap)

  override def withModifiedString(f: String => String)(implicit
      F: Functor[F]
  ): ConsoleLoggerFactory[F] =
    ConsoleLoggerFactory.withModifiedString(this, f)

  override def mapK[G[_]](fk: F ~> G)(implicit F: Functor[F]): ConsoleLoggerFactory[G] =
    ConsoleLoggerFactory.mapK[F, G](fk)(this)
}
object ConsoleLoggerFactory {

  def apply[F[_]: Async: Console](
      defaultLogLevel: LogLevel,
      logLevelOverrides: (String, LogLevel)*
  ): Resource[F, ConsoleLoggerFactory[F]] =
    Dispatcher.sequential[F].evalMap { dispatcher =>
      (
        Ref[F].of(defaultLogLevel),
        Ref[F].of(Chain.fromSeq(logLevelOverrides.map((new LogLevelOverride(_, _)).tupled)))
      ).mapN { (globalLogLevelRef, logLevelOverrides) =>
        new ConsoleLoggerFactory[F] {
          override def setGlobalLogLevel(logLevel: LogLevel): F[Unit] =
            globalLogLevelRef.set(logLevel)

          override def addLogLevelOverride(prefix: String, logLevel: LogLevel): F[Unit] =
            logLevelOverrides.update(_.append(new LogLevelOverride(prefix, logLevel)))

          override def removeLogLevelOverride(prefix: String): F[Unit] =
            logLevelOverrides.update(_.filterNot(_.prefix == prefix))

          override def fromName(name: String): F[SelfAwareStructuredLogger[F]] =
            (globalLogLevelRef.get, logLevelOverrides.get).flatMapN {
              (globalLogLevel, logLevelOverrides) =>
                val logLevel =
                  logLevelOverrides.find(_.matches(name)).fold(globalLogLevel)(_.logLevel)
                ConsoleLogger[F](name, logLevel)
            }.widen

          override def getLoggerFromName(name: String): SelfAwareStructuredLogger[F] =
            dispatcher.unsafeRunSync(fromName(name))
        }
      }
    }

  private class LogLevelOverride(val prefix: String, val logLevel: LogLevel) {
    def matches(name: String): Boolean = name.startsWith(prefix)
  }

  private def mapK[F[_]: Functor, G[_]](
      fk: F ~> G
  )(lf: ConsoleLoggerFactory[F]): ConsoleLoggerFactory[G] =
    new ConsoleLoggerFactory[G] {
      override def setGlobalLogLevel(logLevel: LogLevel): G[Unit] = fk(
        lf.setGlobalLogLevel(logLevel)
      )

      override def addLogLevelOverride(prefix: String, logLevel: LogLevel): G[Unit] =
        fk(lf.addLogLevelOverride(prefix, logLevel))

      override def removeLogLevelOverride(prefix: String): G[Unit] = fk(
        lf.removeLogLevelOverride(prefix)
      )

      override def getLoggerFromName(name: String): SelfAwareStructuredLogger[G] =
        lf.getLoggerFromName(name).mapK(fk)

      override def fromName(name: String): G[SelfAwareStructuredLogger[G]] = fk(
        lf.fromName(name).map(_.mapK(fk))
      )
    }

  private def addContext[F[_]: Functor](
      lf: ConsoleLoggerFactory[F],
      ctx: Map[String, String]
  ): ConsoleLoggerFactory[F] =
    new ConsoleLoggerFactory[F] {
      override def setGlobalLogLevel(logLevel: LogLevel): F[Unit] = lf.setGlobalLogLevel(logLevel)

      override def addLogLevelOverride(prefix: String, logLevel: LogLevel): F[Unit] =
        lf.addLogLevelOverride(prefix, logLevel)

      override def removeLogLevelOverride(prefix: String): F[Unit] =
        lf.removeLogLevelOverride(prefix)

      override def getLoggerFromName(name: String): SelfAwareStructuredLogger[F] =
        lf.getLoggerFromName(name).addContext(ctx)

      override def fromName(name: String): F[SelfAwareStructuredLogger[F]] =
        lf.fromName(name).map(_.addContext(ctx))
    }

  private def withModifiedString[F[_]: Functor](
      lf: ConsoleLoggerFactory[F],
      f: String => String
  ): ConsoleLoggerFactory[F] =
    new ConsoleLoggerFactory[F] {
      override def setGlobalLogLevel(logLevel: LogLevel): F[Unit] = lf.setGlobalLogLevel(logLevel)

      override def addLogLevelOverride(prefix: String, logLevel: LogLevel): F[Unit] =
        lf.addLogLevelOverride(prefix, logLevel)

      override def removeLogLevelOverride(prefix: String): F[Unit] =
        lf.removeLogLevelOverride(prefix)

      override def getLoggerFromName(name: String): SelfAwareStructuredLogger[F] =
        lf.getLoggerFromName(name).withModifiedString(f)

      override def fromName(name: String): F[SelfAwareStructuredLogger[F]] =
        lf.fromName(name).map(_.withModifiedString(f))
    }

}
