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

import cats.*

trait Logger[F[_]] extends MessageLogger[F] with ErrorLogger[F] {
  protected def kernel: LoggerKernel[F, String]
  
  /** Access to the underlying kernel for advanced use cases */
  def underlying: LoggerKernel[F, String] = kernel

  // MessageLogger methods
  def error(message: => String): F[Unit] = 
    kernel.log(KernelLogLevel.Error, _.withMessage(message))
  
  def warn(message: => String): F[Unit] = 
    kernel.log(KernelLogLevel.Warn, _.withMessage(message))
  
  def info(message: => String): F[Unit] = 
    kernel.log(KernelLogLevel.Info, _.withMessage(message))
  
  def debug(message: => String): F[Unit] = 
    kernel.log(KernelLogLevel.Debug, _.withMessage(message))
  
  def trace(message: => String): F[Unit] = 
    kernel.log(KernelLogLevel.Trace, _.withMessage(message))

  // ErrorLogger methods
  def error(t: Throwable)(message: => String): F[Unit] = 
    kernel.log(KernelLogLevel.Error, _.withMessage(message).withThrowable(t))
  
  def warn(t: Throwable)(message: => String): F[Unit] = 
    kernel.log(KernelLogLevel.Warn, _.withMessage(message).withThrowable(t))
  
  def info(t: Throwable)(message: => String): F[Unit] = 
    kernel.log(KernelLogLevel.Info, _.withMessage(message).withThrowable(t))
  
  def debug(t: Throwable)(message: => String): F[Unit] = 
    kernel.log(KernelLogLevel.Debug, _.withMessage(message).withThrowable(t))
  
  def trace(t: Throwable)(message: => String): F[Unit] = 
    kernel.log(KernelLogLevel.Trace, _.withMessage(message).withThrowable(t))

  def withModifiedString(f: String => String): Logger[F] = Logger.withModifiedString[F](this, f)
  override def mapK[G[_]](fk: F ~> G): Logger[G] = Logger.mapK(fk)(this)
}

object Logger {
  def apply[F[_]](implicit ev: Logger[F]) = ev



  private def withModifiedString[F[_]](l: Logger[F], f: String => String): Logger[F] =
    new Logger[F] {
      protected def kernel: LoggerKernel[F, String] = l.underlying
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

  private def mapK[G[_], F[_]](f: G ~> F)(logger: Logger[G]): Logger[F] =
    new Logger[F] {
      protected def kernel: LoggerKernel[F, String] = logger.underlying.mapK(f)
    }
}
