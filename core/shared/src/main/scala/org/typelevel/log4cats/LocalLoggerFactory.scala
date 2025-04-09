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

import cats.mtl.Local
import cats.syntax.functor.*
import cats.{Functor, Monad, Show}
import org.typelevel.otel4s.KindTransformer

sealed trait LocalLoggerFactory[F[_]] extends LoggerFactoryGen[F] {
  final type LoggerType = LocalLogger[F]

  def withAddedContext[A](ctx: Map[String, String])(fa: F[A]): F[A]

  def withAddedContext[A](ctx: (String, Show.Shown)*)(fa: F[A]): F[A]

  def mapK[G[_]: Monad](implicit kt: KindTransformer[F, G]): LocalLoggerFactory[G]

  def withModifiedString(f: String => String)(implicit F: Functor[F]): LocalLoggerFactory[F]
}

object LocalLoggerFactory {
  private[this] final class Impl[F[_]: Monad](
      localLogContext: LocalLogContext[F],
      underlying: LoggerFactory[F]
  ) extends LocalLoggerFactory[F] {
    def withAddedContext[A](ctx: Map[String, String])(fa: F[A]): F[A] =
      localLogContext.withAddedContext(ctx)(fa)
    def withAddedContext[A](ctx: (String, Show.Shown)*)(fa: F[A]): F[A] =
      localLogContext.withAddedContext(ctx*)(fa)

    def mapK[G[_]: Monad](implicit kt: KindTransformer[F, G]): LocalLoggerFactory[G] =
      new Impl(localLogContext.mapK[G], underlying.mapK(kt.liftK))
    def withModifiedString(f: String => String)(implicit F: Functor[F]): LocalLoggerFactory[F] =
      new Impl(localLogContext, underlying.withModifiedString(f))

    def getLoggerFromName(name: String): LocalLogger[F] =
      LocalLogger(localLogContext, underlying.getLoggerFromName(name))
    def fromName(name: String): F[LocalLogger[F]] =
      underlying.fromName(name).map(LocalLogger(localLogContext, _))
  }

  def apply[F[_]: Monad](
      localLogContext: LocalLogContext[F],
      underlying: LoggerFactory[F]
  ): LocalLoggerFactory[F] =
    new Impl(localLogContext, underlying)

  def fromLocal[F[_]: Monad](
      underlying: LoggerFactory[F]
  )(implicit localCtx: Local[F, Map[String, String]]): LocalLoggerFactory[F] =
    apply(LocalLogContext.fromLocal, underlying)
}
