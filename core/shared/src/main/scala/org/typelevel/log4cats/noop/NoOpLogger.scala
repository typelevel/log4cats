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
import org.typelevel.log4cats.{SelfAwareStructuredLogger, LoggerKernel, KernelLogLevel, Log}

object NoOpLogger {
  def apply[F[_]: Applicative]: SelfAwareStructuredLogger[F] = impl[F]

  def strictEvalArgs[F[_]: Applicative]: SelfAwareStructuredLogger[F] =
    impl_[F](evaluateArgs = true)

  def impl[F[_]: Applicative] = impl_[F](evaluateArgs = false)

  private[noop] def impl_[F[_]: Applicative](evaluateArgs: Boolean): SelfAwareStructuredLogger[F] =
    if (evaluateArgs) strictImpl else lazyImpl

  private def lazyImpl[F[_]: Applicative] = new SelfAwareStructuredLogger[F] {
    protected def kernel: LoggerKernel[F, String] = new LoggerKernel[F, String] {
      def log(level: KernelLogLevel, record: Log.Builder[String] => Log.Builder[String]): F[Unit] = 
        Applicative[F].pure(())
    }

    @inline override def isTraceEnabled: F[Boolean] = Applicative[F].pure(false)
    @inline override def isDebugEnabled: F[Boolean] = Applicative[F].pure(false)
    @inline override def isInfoEnabled: F[Boolean] = Applicative[F].pure(false)
    @inline override def isWarnEnabled: F[Boolean] = Applicative[F].pure(false)
    @inline override def isErrorEnabled: F[Boolean] = Applicative[F].pure(false)
  }

  private def strictImpl[F[_]: Applicative] = new SelfAwareStructuredLogger[F] {
    protected def kernel: LoggerKernel[F, String] = new LoggerKernel[F, String] {
      def log(level: KernelLogLevel, record: Log.Builder[String] => Log.Builder[String]): F[Unit] = 
        Applicative[F].pure(())
    }

    @inline override def isTraceEnabled: F[Boolean] = Applicative[F].pure(true)
    @inline override def isDebugEnabled: F[Boolean] = Applicative[F].pure(true)
    @inline override def isInfoEnabled: F[Boolean] = Applicative[F].pure(true)
    @inline override def isWarnEnabled: F[Boolean] = Applicative[F].pure(true)
    @inline override def isErrorEnabled: F[Boolean] = Applicative[F].pure(true)
  }

}
