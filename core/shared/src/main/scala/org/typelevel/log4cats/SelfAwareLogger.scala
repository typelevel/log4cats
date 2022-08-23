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

import cats._

trait SelfAwareLogger[F[_]] extends Logger[F] {
  def isTraceEnabled: F[Boolean]
  def isDebugEnabled: F[Boolean]
  def isInfoEnabled: F[Boolean]
  def isWarnEnabled: F[Boolean]
  def isErrorEnabled: F[Boolean]
  override def mapK[G[_]](fk: F ~> G): SelfAwareLogger[G] = SelfAwareLogger.mapK(fk)(this)

  override def withModifiedString(f: String => String): SelfAwareLogger[F] =
    SelfAwareLogger.withModifiedString[F](this, f)
}
object SelfAwareLogger {
  def apply[F[_]](implicit ev: SelfAwareLogger[F]): SelfAwareLogger[F] = ev

  trait Stubbed[F[_]] extends SelfAwareLogger[F] {
    protected def F: Applicative[F]
    def isTraceEnabled: F[Boolean] = F.pure(true)
    def isDebugEnabled: F[Boolean] = F.pure(true)
    def isInfoEnabled: F[Boolean] = F.pure(true)
    def isWarnEnabled: F[Boolean] = F.pure(true)
    def isErrorEnabled: F[Boolean] = F.pure(true)
  }

  private def mapK[G[_], F[_]](f: G ~> F)(logger: SelfAwareLogger[G]): SelfAwareLogger[F] =
    new SelfAwareLogger[F] {
      def isTraceEnabled: F[Boolean] =
        f(logger.isTraceEnabled)
      def isDebugEnabled: F[Boolean] =
        f(logger.isDebugEnabled)
      def isInfoEnabled: F[Boolean] =
        f(logger.isInfoEnabled)
      def isWarnEnabled: F[Boolean] =
        f(logger.isWarnEnabled)
      def isErrorEnabled: F[Boolean] =
        f(logger.isErrorEnabled)

      def error(t: Throwable)(message: => String): F[Unit] =
        f(logger.error(t)(message))
      def warn(t: Throwable)(message: => String): F[Unit] =
        f(logger.warn(t)(message))
      def info(t: Throwable)(message: => String): F[Unit] =
        f(logger.info(t)(message))
      def debug(t: Throwable)(message: => String): F[Unit] =
        f(logger.debug(t)(message))
      def trace(t: Throwable)(message: => String): F[Unit] =
        f(logger.trace(t)(message))
      def error(message: => String): F[Unit] =
        f(logger.error(message))
      def warn(message: => String): F[Unit] =
        f(logger.warn(message))
      def info(message: => String): F[Unit] =
        f(logger.info(message))
      def debug(message: => String): F[Unit] =
        f(logger.debug(message))
      def trace(message: => String): F[Unit] =
        f(logger.trace(message))
    }

  private def withModifiedString[F[_]](
      l: SelfAwareLogger[F],
      f: String => String
  ): SelfAwareLogger[F] =
    new SelfAwareLogger[F] {
      override def isTraceEnabled: F[Boolean] = l.isTraceEnabled
      override def isDebugEnabled: F[Boolean] = l.isDebugEnabled
      override def isInfoEnabled: F[Boolean] = l.isInfoEnabled
      override def isWarnEnabled: F[Boolean] = l.isWarnEnabled
      override def isErrorEnabled: F[Boolean] = l.isErrorEnabled

      override def error(message: => String): F[Unit] = l.error(f(message))
      override def error(t: Throwable)(message: => String): F[Unit] = l.error(t)(f(message))
      override def warn(message: => String): F[Unit] = l.warn(f(message))
      override def warn(t: Throwable)(message: => String): F[Unit] = l.warn(t)(f(message))
      override def info(message: => String): F[Unit] = l.info(f(message))
      override def info(t: Throwable)(message: => String): F[Unit] = l.info(t)(f(message))
      override def debug(message: => String): F[Unit] = l.debug(f(message))
      override def debug(t: Throwable)(message: => String): F[Unit] = l.debug(t)(f(message))
      override def trace(message: => String): F[Unit] = l.trace(f(message))
      override def trace(t: Throwable)(message: => String): F[Unit] = l.trace(t)(f(message))
    }
}
