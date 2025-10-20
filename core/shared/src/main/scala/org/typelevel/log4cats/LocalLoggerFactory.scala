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

import cats.mtl.{LiftKind, Local}
import cats.syntax.functor.*
import cats.{Functor, Monad, Show}

/** A factory for [[LocalLogger loggers]] with [[cats.mtl.Local `Local`]] semantics. */
sealed trait LocalLoggerFactory[F[_]] extends LoggerFactoryGen[F] {
  final type LoggerType = LocalLogger[F]

  /**
   * Modifies the given effect to have the provided context stored [[cats.mtl.Local locally]].
   *
   * Context added using this method is available to all loggers created by this factory.
   */
  def withAddedContext[A](ctx: Map[String, String])(fa: F[A]): F[A]

  /**
   * Modifies the given effect to have the provided context stored [[cats.mtl.Local locally]].
   *
   * Context added using this method is available to all loggers created by this factory.
   */
  def withAddedContext[A](ctx: (String, Show.Shown)*)(fa: F[A]): F[A]

  /** Lifts this factory's context from `F` to `G`. */
  def liftTo[G[_]](implicit lift: LiftKind[F, G], G: Monad[G]): LocalLoggerFactory[G]

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

    def liftTo[G[_]](implicit lift: LiftKind[F, G], G: Monad[G]): LocalLoggerFactory[G] =
      new Impl(localLogContext.liftTo[G], underlying.mapK(lift))
    def withModifiedString(f: String => String)(implicit F: Functor[F]): LocalLoggerFactory[F] =
      new Impl(localLogContext, underlying.withModifiedString(f))

    def getLoggerFromName(name: String): LocalLogger[F] =
      LocalLogger(localLogContext, underlying.getLoggerFromName(name))
    def fromName(name: String): F[LocalLogger[F]] =
      underlying.fromName(name).map(LocalLogger(localLogContext, _))
  }

  /**
   * @return
   *   a factory for [[cats.mtl.Local local]] loggers backed by the given [[`LocalLogContext`]] and
   *   [[`LoggerFactory`]]
   */
  def apply[F[_]: Monad](
      localLogContext: LocalLogContext[F],
      underlying: LoggerFactory[F]
  ): LocalLoggerFactory[F] =
    new Impl(localLogContext, underlying)

  /**
   * @return
   *   a factory for local loggers backed by the given [[`LoggerFactory`]] and implicit
   *   [[cats.mtl.Local `Local`]]
   */
  def fromLocal[F[_]: Monad](
      underlying: LoggerFactory[F]
  )(implicit localCtx: Local[F, Map[String, String]]): LocalLoggerFactory[F] =
    apply(LocalLogContext.fromLocal, underlying)
}
