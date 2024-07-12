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

package org.typelevel.log4cats.noop

import cats.Applicative
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.extras.LogLevel

object NoOpLogger {
  def apply[F[_]: Applicative]: SelfAwareStructuredLogger[F] = impl[F]

  def strictEvalArgs[F[_]: Applicative]: SelfAwareStructuredLogger[F] =
    impl_[F](evaluateArgs = true)

  def impl[F[_]: Applicative]: SelfAwareStructuredLogger[F] = impl_[F](evaluateArgs = false)

  private[noop] def impl_[F[_]: Applicative](evaluateArgs: Boolean): SelfAwareStructuredLogger[F] =
    if (evaluateArgs) strictImpl else lazyImpl

  private def lazyImpl[F[_]: Applicative]: SelfAwareStructuredLogger[F] =
    new SelfAwareStructuredLogger[F] {

      val no: F[Boolean] = Applicative[F].pure(false)
      val unit: F[Unit] = Applicative[F].pure(())

      @inline override def isEnabled(ll: LogLevel): F[Boolean] = no
      @inline override def log(ll: LogLevel, ctx: Map[String, String], msg: => String): F[Unit] =
        unit
      @inline override def log(
          ll: LogLevel,
          ctx: Map[String, String],
          t: Throwable,
          msg: => String
      ): F[Unit] = unit
      @inline override def log(ll: LogLevel, t: Throwable, msg: => String): F[Unit] = unit
      @inline override def log(ll: LogLevel, msg: => String): F[Unit] = unit
    }

  private def strictImpl[F[_]: Applicative]: SelfAwareStructuredLogger[F] =
    new SelfAwareStructuredLogger[F] {

      val yes: F[Boolean] = Applicative[F].pure(true)
      def void(arg: => String): F[Unit] = Applicative[F].pure {
        val _ = arg
        ()
      }

      @inline override def isEnabled(ll: LogLevel): F[Boolean] = yes
      @inline override def log(ll: LogLevel, ctx: Map[String, String], msg: => String): F[Unit] =
        void(msg)
      @inline override def log(
          ll: LogLevel,
          ctx: Map[String, String],
          t: Throwable,
          msg: => String
      ): F[Unit] = void(msg)
      @inline override def log(ll: LogLevel, t: Throwable, msg: => String): F[Unit] = void(msg)
      @inline override def log(ll: LogLevel, msg: => String): F[Unit] = void(msg)
    }

}
