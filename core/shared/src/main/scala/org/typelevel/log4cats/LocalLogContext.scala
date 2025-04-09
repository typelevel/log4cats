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

import cats.mtl.{Ask, Local}
import cats.syntax.functor.*
import cats.syntax.traverse.*
import cats.{~>, Applicative, Show}
import org.typelevel.otel4s.KindTransformer

import scala.collection.immutable.ArraySeq

sealed trait LocalLogContext[F[_]] {
  private[log4cats] def currentLogContext: F[Map[String, String]]

  private[log4cats] def withAddedContext[A](ctx: Map[String, String])(fa: F[A]): F[A]

  private[log4cats] final def withAddedContext[A](ctx: (String, Show.Shown)*)(fa: F[A]): F[A] =
    withAddedContext {
      ctx.view.map { case (k, v) => k -> v.toString }.toMap
    }(fa)

  def withHighPriorityAskedContext(ask: Ask[F, Map[String, String]]): LocalLogContext[F]

  def withLowPriorityAskedContext(ask: Ask[F, Map[String, String]]): LocalLogContext[F]

  def mapK[G[_]: Applicative](implicit kt: KindTransformer[F, G]): LocalLogContext[G]
}

object LocalLogContext {
  private[this] type AskContext[F[_]] = Ask[F, Map[String, String]]

  private[this] final class MappedKLocal[F[_], G[_], E](
      localF: Local[F, E]
  )(implicit
      val applicative: Applicative[G],
      kt: KindTransformer[F, G]
  ) extends Local[G, E] {
    def ask[E2 >: E]: G[E2] =
      kt.liftK(localF.ask[E2])
    def local[A](ga: G[A])(f: E => E): G[A] =
      kt.limitedMapK(ga) {
        new (F ~> F) {
          def apply[B](fb: F[B]): F[B] = localF.local(fb)(f)
        }
      }
  }

  private[this] final class MappedKAsk[F[_], G[_], E](
      askF: Ask[F, E],
      fk: F ~> G
  )(implicit val applicative: Applicative[G])
      extends Ask[G, E] {
    def ask[E2 >: E]: G[E2] = fk(askF.ask[E2])
  }

  private[this] final class MultiAskContext[F[_]] private[MultiAskContext] (
      asks: Seq[AskContext[F]] /* never empty */
  ) extends AskContext[F] {
    implicit def applicative: Applicative[F] = asks.head.applicative
    def ask[E2 >: Map[String, String]]: F[E2] =
      asks
        .traverse(_.ask[Map[String, String]])
        .map(_.reduceLeft(_ ++ _))
    def prependLowPriority(ask: AskContext[F]): MultiAskContext[F] =
      new MultiAskContext(ask +: asks)
    def appendHighPriority(ask: AskContext[F]): MultiAskContext[F] =
      new MultiAskContext(asks :+ ask)
  }

  private[this] object MultiAskContext {
    def apply[F[_]](ask: AskContext[F]): MultiAskContext[F] =
      ask match {
        case multi: MultiAskContext[F] => multi
        case other => new MultiAskContext(ArraySeq(other))
      }
  }

  private[this] final class Impl[F[_]](
      localCtx: Local[F, Map[String, String]],
      askCtx: AskContext[F]
  ) extends LocalLogContext[F] {
    private[log4cats] def currentLogContext: F[Map[String, String]] =
      askCtx.ask[Map[String, String]]
    private[log4cats] def withAddedContext[A](ctx: Map[String, String])(fa: F[A]): F[A] =
      localCtx.local(fa)(_ ++ ctx)

    def withHighPriorityAskedContext(ask: Ask[F, Map[String, String]]): LocalLogContext[F] =
      new Impl(
        localCtx,
        MultiAskContext(askCtx).appendHighPriority(ask)
      )

    def withLowPriorityAskedContext(ask: Ask[F, Map[String, String]]): LocalLogContext[F] =
      new Impl(
        localCtx,
        MultiAskContext(askCtx).prependLowPriority(ask)
      )

    def mapK[G[_]](implicit G: Applicative[G], kt: KindTransformer[F, G]): LocalLogContext[G] = {
      val localF = localCtx
      val askF = askCtx
      val localG = new MappedKLocal(localF)
      val askG =
        if (askF eq localF) localG
        else new MappedKAsk(askF, kt.liftK)
      new Impl(localG, askG)
    }
  }

  def fromLocal[F[_]](implicit localCtx: Local[F, Map[String, String]]): LocalLogContext[F] =
    new Impl(localCtx, localCtx)
}
