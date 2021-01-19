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
import cats.data.{EitherT, OptionT}

trait Logger[F[_]] extends MessageLogger[F] with ErrorLogger[F] {
  def withModifiedString(f: String => String): Logger[F] = Logger.withModifiedString[F](this, f)
  override def mapK[G[_]](fk: F ~> G): Logger[G] = Logger.mapK(fk)(this)
}

object Logger {
  def apply[F[_]](implicit ev: Logger[F]) = ev

  implicit def optionTLogger[F[_]: Logger: Functor]: Logger[OptionT[F, *]] =
    Logger[F].mapK(OptionT.liftK[F])

  implicit def eitherTLogger[F[_]: Logger: Functor, E]: Logger[EitherT[F, E, *]] =
    Logger[F].mapK(EitherT.liftK[F, E])

  private def withModifiedString[F[_]](l: Logger[F], f: String => String): Logger[F] =
    new Logger[F] {
      def error(message: => String): F[Unit] = l.error(f(message))
      def error(t: Throwable)(message: => String): F[Unit] = l.error(t)(f(message))
      def warn(message: => String): F[Unit] = l.warn(f(message))
      def warn(t: Throwable)(message: => String): F[Unit] = l.warn(t)(f(message))
      def info(message: => String): F[Unit] = l.info(f(message))
      def info(t: Throwable)(message: => String): F[Unit] = l.info(t)(f(message))
      def debug(message: => String): F[Unit] = l.debug(f(message))
      def debug(t: Throwable)(message: => String): F[Unit] = l.debug(t)(f(message))
      def trace(message: => String): F[Unit] = l.trace(f(message))
      def trace(t: Throwable)(message: => String): F[Unit] = l.trace(t)(f(message))
    }

  private def mapK[G[_], F[_]](f: G ~> F)(logger: Logger[G]): Logger[F] =
    new Logger[F] {
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
    }

}
