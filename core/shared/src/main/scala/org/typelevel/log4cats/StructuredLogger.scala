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

trait StructuredLogger[F[_]] extends Logger[F] {
  def trace(ctx: Map[String, String])(msg: => String): F[Unit]
  def trace(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit]
  def debug(ctx: Map[String, String])(msg: => String): F[Unit]
  def debug(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit]
  def info(ctx: Map[String, String])(msg: => String): F[Unit]
  def info(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit]
  def warn(ctx: Map[String, String])(msg: => String): F[Unit]
  def warn(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit]
  def error(ctx: Map[String, String])(msg: => String): F[Unit]
  def error(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit]
  override def mapK[G[_]](fk: F ~> G): StructuredLogger[G] =
    StructuredLogger.mapK(fk)(this)
}

object StructuredLogger {
  def apply[F[_]](implicit ev: StructuredLogger[F]): StructuredLogger[F] = ev

  def withContext[F[_]](sl: StructuredLogger[F])(ctx: Map[String, String]): StructuredLogger[F] =
    new ModifiedContextStructuredLogger[F](sl)(ctx ++ _)

  def withModifiedContext[F[_]](
      sl: StructuredLogger[F]
  )(modifyCtx: Map[String, String] => Map[String, String]): StructuredLogger[F] =
    new ModifiedContextStructuredLogger[F](sl)(modifyCtx)

  private class ModifiedContextStructuredLogger[F[_]](sl: StructuredLogger[F])(
      modify: Map[String, String] => Map[String, String]
  ) extends StructuredLogger[F] {
    private lazy val defaultCtx: Map[String, String] = modify(Map.empty)
    def error(message: => String): F[Unit] = sl.error(defaultCtx)(message)
    def warn(message: => String): F[Unit] = sl.warn(defaultCtx)(message)
    def info(message: => String): F[Unit] = sl.info(defaultCtx)(message)
    def debug(message: => String): F[Unit] = sl.debug(defaultCtx)(message)
    def trace(message: => String): F[Unit] = sl.trace(defaultCtx)(message)
    def trace(ctx: Map[String, String])(msg: => String): F[Unit] =
      sl.trace(modify(ctx))(msg)
    def debug(ctx: Map[String, String])(msg: => String): F[Unit] =
      sl.debug(modify(ctx))(msg)
    def info(ctx: Map[String, String])(msg: => String): F[Unit] =
      sl.info(modify(ctx))(msg)
    def warn(ctx: Map[String, String])(msg: => String): F[Unit] =
      sl.warn(modify(ctx))(msg)
    def error(ctx: Map[String, String])(msg: => String): F[Unit] =
      sl.error(modify(ctx))(msg)

    def error(t: Throwable)(message: => String): F[Unit] =
      sl.error(defaultCtx, t)(message)
    def warn(t: Throwable)(message: => String): F[Unit] =
      sl.warn(defaultCtx, t)(message)
    def info(t: Throwable)(message: => String): F[Unit] =
      sl.info(defaultCtx, t)(message)
    def debug(t: Throwable)(message: => String): F[Unit] =
      sl.debug(defaultCtx, t)(message)
    def trace(t: Throwable)(message: => String): F[Unit] =
      sl.trace(defaultCtx, t)(message)

    def error(ctx: Map[String, String], t: Throwable)(message: => String): F[Unit] =
      sl.error(modify(ctx), t)(message)
    def warn(ctx: Map[String, String], t: Throwable)(message: => String): F[Unit] =
      sl.warn(modify(ctx), t)(message)
    def info(ctx: Map[String, String], t: Throwable)(message: => String): F[Unit] =
      sl.info(modify(ctx), t)(message)
    def debug(ctx: Map[String, String], t: Throwable)(message: => String): F[Unit] =
      sl.debug(modify(ctx), t)(message)
    def trace(ctx: Map[String, String], t: Throwable)(message: => String): F[Unit] =
      sl.trace(modify(ctx), t)(message)
  }

  private def mapK[G[_], F[_]](f: G ~> F)(logger: StructuredLogger[G]): StructuredLogger[F] =
    new StructuredLogger[F] {
      def trace(ctx: Map[String, String])(msg: => String): F[Unit] =
        f(logger.trace(ctx)(msg))
      def debug(ctx: Map[String, String])(msg: => String): F[Unit] =
        f(logger.debug(ctx)(msg))
      def info(ctx: Map[String, String])(msg: => String): F[Unit] =
        f(logger.info(ctx)(msg))
      def warn(ctx: Map[String, String])(msg: => String): F[Unit] =
        f(logger.warn(ctx)(msg))
      def error(ctx: Map[String, String])(msg: => String): F[Unit] =
        f(logger.error(ctx)(msg))

      def error(t: Throwable)(message: => String): F[Unit] =
        f(logger.error(t)(message))
      def warn(t: Throwable)(message: => String): F[Unit] =
        f(logger.warn(t)(message))
      def info(t: Throwable)(message: => String): F[Unit] =
        f(logger.info(t)(message))
      def debug(t: Throwable)(message: => String): F[Unit] =
        f(logger.debug(t)(message))
      def trace(t: Throwable)(message: => String): F[Unit] =
        f(logger.trace(t)(message))
      def error(message: => String): F[Unit] =
        f(logger.error(message))
      def warn(message: => String): F[Unit] =
        f(logger.warn(message))
      def info(message: => String): F[Unit] =
        f(logger.info(message))
      def debug(message: => String): F[Unit] =
        f(logger.debug(message))
      def trace(message: => String): F[Unit] =
        f(logger.trace(message))

      def trace(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
        f(logger.trace(ctx, t)(msg))
      def debug(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
        f(logger.debug(ctx, t)(msg))
      def info(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
        f(logger.info(ctx, t)(msg))
      def warn(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
        f(logger.warn(ctx, t)(msg))
      def error(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
        f(logger.error(ctx, t)(msg))
    }
}
