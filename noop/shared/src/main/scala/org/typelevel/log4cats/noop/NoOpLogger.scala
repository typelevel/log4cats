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

object NoOpLogger {
  def apply[F[_]: Applicative]: SelfAwareStructuredLogger[F] = impl[F]

  def strictEvalArgs[F[_]: Applicative]: SelfAwareStructuredLogger[F] =
    impl_[F](evaluateArgs = true)

  def impl[F[_]: Applicative] = impl_[F](evaluateArgs = false)

  private[noop] def impl_[F[_]: Applicative](evaluateArgs: Boolean): SelfAwareStructuredLogger[F] =
    if (evaluateArgs) stricImpl else lazyImpl

  private def lazyImpl[F[_]: Applicative] = new SelfAwareStructuredLogger[F] {

    val no: F[Boolean] = Applicative[F].pure(false)
    val unit: F[Unit] = Applicative[F].pure(())

    @inline override def isTraceEnabled: F[Boolean] = no
    @inline override def isDebugEnabled: F[Boolean] = no
    @inline override def isInfoEnabled: F[Boolean] = no
    @inline override def isWarnEnabled: F[Boolean] = no
    @inline override def isErrorEnabled: F[Boolean] = no
    @inline override def trace(t: Throwable)(msg: => String): F[Unit] = unit
    @inline override def trace(msg: => String): F[Unit] = unit
    @inline override def trace(ctx: Map[String, String])(msg: => String): F[Unit] = unit
    @inline override def debug(t: Throwable)(msg: => String): F[Unit] = unit
    @inline override def debug(msg: => String): F[Unit] = unit
    @inline override def debug(ctx: Map[String, String])(msg: => String): F[Unit] = unit
    @inline override def info(t: Throwable)(msg: => String): F[Unit] = unit
    @inline override def info(msg: => String): F[Unit] = unit
    @inline override def info(ctx: Map[String, String])(msg: => String): F[Unit] = unit
    @inline override def warn(t: Throwable)(msg: => String): F[Unit] = unit
    @inline override def warn(msg: => String): F[Unit] = unit
    @inline override def warn(ctx: Map[String, String])(msg: => String): F[Unit] = unit
    @inline override def error(t: Throwable)(msg: => String): F[Unit] = unit
    @inline override def error(msg: => String): F[Unit] = unit
    @inline override def error(ctx: Map[String, String])(msg: => String): F[Unit] = unit
    @inline override def trace(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
      unit
    @inline override def debug(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
      unit
    @inline override def info(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
      unit
    @inline override def warn(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
      unit
    @inline override def error(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
      unit
  }

  private def stricImpl[F[_]: Applicative] = new SelfAwareStructuredLogger[F] {

    val no: F[Boolean] = Applicative[F].pure(false)
    def void(arg: => Any): F[Unit] = Applicative[F].pure {
      val _ = arg
      ()
    }

    @inline override def isTraceEnabled: F[Boolean] = no
    @inline override def isDebugEnabled: F[Boolean] = no
    @inline override def isInfoEnabled: F[Boolean] = no
    @inline override def isWarnEnabled: F[Boolean] = no
    @inline override def isErrorEnabled: F[Boolean] = no
    @inline override def trace(t: Throwable)(msg: => String): F[Unit] = void(t)
    @inline override def trace(msg: => String): F[Unit] = void(msg)
    @inline override def trace(ctx: Map[String, String])(msg: => String): F[Unit] = void(msg)
    @inline override def debug(t: Throwable)(msg: => String): F[Unit] = void(msg)
    @inline override def debug(msg: => String): F[Unit] = void(msg)
    @inline override def debug(ctx: Map[String, String])(msg: => String): F[Unit] = void(msg)
    @inline override def info(t: Throwable)(msg: => String): F[Unit] = void(msg)
    @inline override def info(msg: => String): F[Unit] = void(msg)
    @inline override def info(ctx: Map[String, String])(msg: => String): F[Unit] = void(msg)
    @inline override def warn(t: Throwable)(msg: => String): F[Unit] = void(msg)
    @inline override def warn(msg: => String): F[Unit] = void(msg)
    @inline override def warn(ctx: Map[String, String])(msg: => String): F[Unit] = void(msg)
    @inline override def error(t: Throwable)(msg: => String): F[Unit] = void(msg)
    @inline override def error(msg: => String): F[Unit] = void(msg)
    @inline override def error(ctx: Map[String, String])(msg: => String): F[Unit] = void(msg)
    @inline override def trace(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
      void(msg)
    @inline override def debug(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
      void(msg)
    @inline override def info(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
      void(msg)
    @inline override def warn(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
      void(msg)
    @inline override def error(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
      void(msg)
  }

}
