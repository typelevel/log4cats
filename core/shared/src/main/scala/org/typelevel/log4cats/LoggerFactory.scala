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

import cats.Functor
import cats.Show.Shown
import cats.data.Kleisli
import cats.syntax.functor.*
import cats.~>
import cats.data.OptionT
import cats.data.EitherT

import scala.annotation.implicitNotFound

@implicitNotFound("""
Implicit not found for LoggerFactory[${F}].
Learn about LoggerFactory at https://typelevel.org/log4cats/#logging-using-capabilities
""")
trait LoggerFactory[F[_]] extends LoggerFactoryGen[F] {
  type LoggerType = SelfAwareStructuredLogger[F]

  def mapK[G[_]](fk: F ~> G)(implicit F: Functor[F]): LoggerFactory[G] =
    LoggerFactory.mapK[F, G](fk)(this)

  def addContext(ctx: Map[String, String])(implicit F: Functor[F]): LoggerFactory[F] =
    LoggerFactory.addContext(this, ctx)

  def addContext(pairs: (String, Shown)*)(implicit F: Functor[F]): LoggerFactory[F] =
    addContext(pairs.map { case (k, v) => (k, v.toString) }.toMap)

  def withModifiedString(f: String => String)(implicit F: Functor[F]): LoggerFactory[F] =
    LoggerFactory.withModifiedString(this, f)
}

object LoggerFactory extends LoggerFactoryGenCompanion {
  def apply[F[_]: LoggerFactory]: LoggerFactory[F] = implicitly

  implicit def optionTFactory[F[_]: LoggerFactory: Functor]: LoggerFactory[OptionT[F, *]] =
    LoggerFactory[F].mapK(OptionT.liftK[F])

  implicit def eitherTFactory[F[_]: LoggerFactory: Functor, E]: LoggerFactory[EitherT[F, E, *]] =
    LoggerFactory[F].mapK(EitherT.liftK[F, E])

  implicit def kleisliFactory[F[_]: LoggerFactory: Functor, A]: LoggerFactory[Kleisli[F, A, *]] =
    LoggerFactory[F].mapK(Kleisli.liftK[F, A])

  private def mapK[F[_]: Functor, G[_]](fk: F ~> G)(lf: LoggerFactory[F]): LoggerFactory[G] =
    new LoggerFactory[G] {

      def getLoggerFromName(name: String): LoggerType = lf
        .getLoggerFromName(name)
        .mapK(fk)

      def fromName(name: String): G[LoggerType] = {
        val logger = lf.fromName(name).map(_.mapK(fk))
        fk(logger)
      }
    }

  private def addContext[F[_]: Functor](
      lf: LoggerFactory[F],
      ctx: Map[String, String]
  ): LoggerFactory[F] =
    new LoggerFactory[F] {
      override def getLoggerFromName(name: String): SelfAwareStructuredLogger[F] =
        lf.getLoggerFromName(name).addContext(ctx)

      override def fromName(name: String): F[SelfAwareStructuredLogger[F]] =
        lf.fromName(name).map(_.addContext(ctx))
    }

  private def withModifiedString[F[_]: Functor](
      lf: LoggerFactory[F],
      f: String => String
  ): LoggerFactory[F] =
    new LoggerFactory[F] {
      override def getLoggerFromName(name: String): SelfAwareStructuredLogger[F] =
        lf.getLoggerFromName(name).withModifiedString(f)

      override def fromName(name: String): F[SelfAwareStructuredLogger[F]] =
        lf.fromName(name).map(_.withModifiedString(f))
    }

}
