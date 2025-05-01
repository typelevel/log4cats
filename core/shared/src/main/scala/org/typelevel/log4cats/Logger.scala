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
import org.typelevel.log4cats.extras.LogLevel

trait Logger[F[_]] extends MessageLogger[F] with ErrorLogger[F] {
  def withModifiedString(f: String => String): Logger[F] = Logger.withModifiedString[F](this, f)
  override def mapK[G[_]](fk: F ~> G): Logger[G] = Logger.mapK(fk)(this)
}

object Logger {
  def apply[F[_]](implicit ev: Logger[F]): Logger[F] = ev

  implicit def optionTLogger[F[_]: Logger: Functor]: Logger[OptionT[F, *]] =
    Logger[F].mapK(OptionT.liftK[F])

  implicit def eitherTLogger[F[_]: Logger: Functor, E]: Logger[EitherT[F, E, *]] =
    Logger[F].mapK(EitherT.liftK[F, E])

  implicit def kleisliLogger[F[_]: Logger, A]: Logger[Kleisli[F, A, *]] =
    Logger[F].mapK(Kleisli.liftK[F, A])

  private def withModifiedString[F[_]](l: Logger[F], f: String => String): Logger[F] =
    new Logger[F] {
      override def log(ll: LogLevel, t: Throwable, msg: => String): F[Unit] = l.log(ll, t, f(msg))
      override def log(ll: LogLevel, msg: => String): F[Unit] = l.log(ll, f(msg))
    }

  private def mapK[G[_], F[_]](f: G ~> F)(logger: Logger[G]): Logger[F] =
    new Logger[F] {
      override def log(ll: LogLevel, t: Throwable, msg: => String): F[Unit] = f(
        logger.log(ll, t, msg)
      )
      override def log(ll: LogLevel, msg: => String): F[Unit] = f(logger.log(ll, msg))
    }

}
