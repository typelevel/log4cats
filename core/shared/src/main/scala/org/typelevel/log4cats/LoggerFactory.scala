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

import cats.{~>, Applicative, Functor}
import cats.syntax.functor._

import scala.annotation.implicitNotFound

@implicitNotFound("""
Implicit not found for LoggerFactory[${F}].
Learn about LoggerFactory at https://typelevel.org/log4cats/#logging-using-capabilities
""")
trait LoggerFactory[F[_]] extends LoggerFactoryGen[F] {
  type LoggerType = SelfAwareStructuredLogger[F]

  def mapK[G[_]](fk: F ~> G)(implicit F: Functor[F]): LoggerFactory[G] =
    LoggerFactory.mapK[F, G](fk)(this)
}

object LoggerFactory extends LoggerFactoryGenCompanion {
  def apply[F[_]: LoggerFactory]: LoggerFactory[F] = implicitly

  def const[F[_]](
      logger: SelfAwareStructuredLogger[F]
  )(implicit F: Applicative[F]): LoggerFactory[F] = new LoggerFactory[F] {
    override def getLoggerFromName(name: String): SelfAwareStructuredLogger[F] = logger

    override def fromName(name: String): F[SelfAwareStructuredLogger[F]] = F.pure(logger)
  }

  def liftF[F[_]: Applicative](f: String => F[Unit]): LoggerFactory[F] = const(
    SelfAwareStructuredLogger.liftF(f)
  )

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
}
