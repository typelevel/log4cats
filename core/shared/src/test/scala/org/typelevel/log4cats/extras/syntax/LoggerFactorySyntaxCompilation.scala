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
