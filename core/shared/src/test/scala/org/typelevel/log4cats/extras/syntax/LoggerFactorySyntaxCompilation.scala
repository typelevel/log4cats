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

package org.typelevel.log4cats.extras.syntax

import cats._
import cats.data.Kleisli
import org.typelevel.log4cats.LoggerFactory

object LoggerFactorySyntaxCompilation {
  def loggerFactorySyntaxMapK[F[_]: Functor, G[_]](lf: LoggerFactory[F])(
      fk: F ~> G
  ): LoggerFactory[G] =
    lf.mapK[G](fk)

  def loggerFactoryKleisliLiftK[F[_]: Functor, A](
      lf: LoggerFactory[F]
  ): LoggerFactory[Kleisli[F, A, *]] =
    lf.mapK(Kleisli.liftK[F, A])
}
