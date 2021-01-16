/*
 * Copyright 2020 Christopher Davenport
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
}
object SelfAwareLogger {
  def apply[F[_]](implicit ev: SelfAwareLogger[F]): SelfAwareLogger[F] = ev

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
}
