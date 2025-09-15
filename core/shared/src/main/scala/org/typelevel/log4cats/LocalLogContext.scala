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

import cats.mtl.{Ask, LiftKind, Local}
import cats.syntax.functor.*
import cats.syntax.traverse.*
import cats.{Applicative, Show}

import scala.collection.compat.immutable.ArraySeq

/**
 * Log context stored in a [[cats.mtl.Local `Local`]], as well as potentially additional log context
 * provided by [[cats.mtl.Ask `Ask`s]].
 */
sealed trait LocalLogContext[F[_]] {

  /**
   * @return
   *   the current log context stored [[cats.mtl.Local locally]], as well as the context from any
   *   provided [[cats.mtl.Ask `Ask`]]s
   */
  private[log4cats] def currentLogContext: F[Map[String, String]]

  /**
   * @return
   *   the given effect modified to have the provided context stored [[cats.mtl.Local locally]]
   */
  private[log4cats] def withAddedContext[A](ctx: Map[String, String])(fa: F[A]): F[A]

  /**
   * @return
   *   the given effect modified to have the provided context stored [[cats.mtl.Local locally]]
   */
  private[log4cats] final def withAddedContext[A](ctx: (String, Show.Shown)*)(fa: F[A]): F[A] =
    withAddedContext {
      ctx.view.map { case (k, v) => k -> v.toString }.toMap
    }(fa)

  /**
   * Modifies this [[cats.mtl.Local local]] log context to include the context provided by the given
   * [[cats.mtl.Ask `Ask`]] with higher priority than all of its current context; that is, if both
   * the `Ask` and this local log context provide values for some key, the value from the `Ask` will
   * be used. The context is asked for at every logging operation.
   */
  def withHighPriorityAskedContext(ask: Ask[F, Map[String, String]]): LocalLogContext[F]

  /**
   * Modifies this [[cats.mtl.Local local]] log context to include the context provided by the given
   * [[cats.mtl.Ask `Ask`]] with lower priority than all of its current context; that is, if both
   * the `Ask` and this local log context provide values for some key, the value from this local log
   * context will be used. The context is asked for at every logging operation.
   */
  def withLowPriorityAskedContext(ask: Ask[F, Map[String, String]]): LocalLogContext[F]

  /** Lifts this [[cats.mtl.Local local]] log context from `F` to `G`. */
  def liftTo[G[_]](implicit lift: LiftKind[F, G]): LocalLogContext[G]
}

object LocalLogContext {
  private[this] type AskContext[F[_]] = Ask[F, Map[String, String]]

  private[this] final class MultiAskContext[F[_]] private[MultiAskContext] (
      asks: Seq[AskContext[F]] /* never empty */
  ) extends AskContext[F] {
    implicit def applicative: Applicative[F] = asks.head.applicative
    def ask[E2 >: Map[String, String]]: F[E2] =
      asks
        .traverse(_.ask[Map[String, String]])
        .map[Map[String, String]](_.reduceLeft(_ ++ _))
        .widen // tparam on `map` and `widen` to make scala 3 happy
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

    def liftTo[G[_]](implicit lift: LiftKind[F, G]): LocalLogContext[G] = {
      val localF = localCtx
      val askF = askCtx
      val localG = localF.liftTo[G]
      val askG =
        if (askF eq localF) localG
        else askF.liftTo[G]
      new Impl(localG, askG)
    }
  }

  /** @return a `LocalLogContext` backed by the given implicit [[cats.mtl.Local `Local`]] */
  def fromLocal[F[_]](implicit localCtx: Local[F, Map[String, String]]): LocalLogContext[F] =
    new Impl(localCtx, localCtx)
}
