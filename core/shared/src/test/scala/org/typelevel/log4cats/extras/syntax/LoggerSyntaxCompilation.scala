/*
 * Copyright 2018 Christopher Davenport
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
import cats.data.{EitherT, Kleisli, OptionT}
import org.typelevel.log4cats._

object LoggerSyntaxCompilation {

  def errorLoggerSyntaxMapK[F[_], G[_]](l: ErrorLogger[F])(f: F ~> G) =
    l.mapK(f)

  def messageLoggerSyntaxMapK[F[_], G[_]](l: MessageLogger[F])(f: F ~> G) =
    l.mapK(f)

  def loggerSyntaxMapK[F[_], G[_]](l: Logger[F])(f: F ~> G) =
    l.mapK(f)

  def selfAwareLoggerSyntaxMapK[F[_], G[_]](l: SelfAwareLogger[F])(f: F ~> G) =
    l.mapK(f)

  def structuredLoggerMapK[F[_], G[_]](l: StructuredLogger[F])(f: F ~> G) =
    l.mapK(f)

  def selfAwareStructuredLoggerMapK[F[_], G[_]](l: SelfAwareStructuredLogger[F])(f: F ~> G) =
    l.mapK(f)

  def loggerSyntaxWithModifiedString[F[_]](l: Logger[F]): Logger[F] = l.withModifiedString(identity)

  def selfAwareLoggerSyntaxWithModifiedString[F[_]](l: SelfAwareLogger[F]): SelfAwareLogger[F] =
    l.withModifiedString(identity)

  def structuredLoggerSyntaxWithModifiedString[F[_]](l: StructuredLogger[F]): StructuredLogger[F] =
    l.withModifiedString(identity)

  def selfAwareStructuredLoggerSyntaxWithModifiedString[F[_]](
      l: SelfAwareStructuredLogger[F]
  ): SelfAwareStructuredLogger[F] =
    l.withModifiedString(identity)

  def eitherTLogger[F[_]: Functor: Logger, A]: EitherT[F, A, Unit] =
    Logger[EitherT[F, A, *]].info("foo")

  def kleisliLogger[F[_]: Logger, A]: Kleisli[F, A, Unit] =
    Logger[Kleisli[F, A, *]].info("bar")

  def optionTLogger[F[_]: Functor: Logger]: OptionT[F, Unit] =
    Logger[OptionT[F, *]].info("baz")

}
