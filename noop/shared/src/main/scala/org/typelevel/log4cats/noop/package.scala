package org.typelevel.log4cats

import cats.Applicative

package object noop {
  implicit def loggerFactoryForApplicative[F[_]: Applicative]: NoOpFactory[F] = new NoOpFactory
}
