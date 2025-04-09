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

import cats.mtl.Local
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.{~>, Monad, Show}
import org.typelevel.otel4s.KindTransformer

sealed trait LocalLogger[F[_]] extends SelfAwareLogger[F] {
  def withAddedContext[A](ctx: Map[String, String])(fa: F[A]): F[A]

  def withAddedContext[A](ctx: (String, Show.Shown)*)(fa: F[A]): F[A]

  def error(ctx: Map[String, String])(msg: => String): F[Unit]
  def error(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit]
  def warn(ctx: Map[String, String])(msg: => String): F[Unit]
  def warn(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit]
  def info(ctx: Map[String, String])(msg: => String): F[Unit]
  def info(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit]
  def debug(ctx: Map[String, String])(msg: => String): F[Unit]
  def debug(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit]
  def trace(ctx: Map[String, String])(msg: => String): F[Unit]
  def trace(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit]

  @deprecated(
    "use the overload that takes a `KindTransformer` and `Monad` instead",
    since = "2.8.0"
  )
  override def mapK[G[_]](fk: F ~> G): SelfAwareLogger[G] = super.mapK(fk)

  def mapK[G[_]: Monad](implicit kt: KindTransformer[F, G]): LocalLogger[G]

  override def withModifiedString(f: String => String): LocalLogger[F]

  @deprecated(
    "`StructuredLogger` is cumbersome and lacks `cats.mtl.Local` semantics",
    since = "2.8.0"
  )
  def asStructuredLogger: SelfAwareStructuredLogger[F]
}

object LocalLogger {
  private[this] final class Impl[F[_]](
      localLogContext: LocalLogContext[F],
      underlying: SelfAwareStructuredLogger[F]
  )(implicit F: Monad[F])
      extends LocalLogger[F]
      with SelfAwareStructuredLogger[F] {
    def withAddedContext[A](ctx: Map[String, String])(fa: F[A]): F[A] =
      localLogContext.withAddedContext(ctx)(fa)
    def withAddedContext[A](ctx: (String, Show.Shown)*)(fa: F[A]): F[A] =
      localLogContext.withAddedContext(ctx*)(fa)

    override def addContext(ctx: Map[String, String]): Impl[F] =
      new Impl(localLogContext, underlying.addContext(ctx))
    override def addContext(pairs: (String, Show.Shown)*): Impl[F] =
      new Impl(localLogContext, underlying.addContext(pairs*))

    def isErrorEnabled: F[Boolean] = underlying.isErrorEnabled
    def isWarnEnabled: F[Boolean] = underlying.isWarnEnabled
    def isInfoEnabled: F[Boolean] = underlying.isInfoEnabled
    def isDebugEnabled: F[Boolean] = underlying.isDebugEnabled
    def isTraceEnabled: F[Boolean] = underlying.isTraceEnabled

    @deprecated(
      "use the overload that takes a `KindTransformer` and `Monad` instead",
      since = "2.8.0"
    )
    override def mapK[G[_]](fk: F ~> G): SelfAwareStructuredLogger[G] =
      super.mapK(fk)
    def mapK[G[_]: Monad](implicit kt: KindTransformer[F, G]): LocalLogger[G] =
      new Impl(localLogContext.mapK[G], underlying.mapK(kt.liftK))
    override def withModifiedString(f: String => String): Impl[F] =
      new Impl(localLogContext, underlying.withModifiedString(f))

    @deprecated(
      "`StructuredLogger` is cumbersome and lacks `cats.mtl.Local` semantics",
      since = "2.8.0"
    )
    def asStructuredLogger: SelfAwareStructuredLogger[F] = this

    def error(message: => String): F[Unit] =
      underlying.isErrorEnabled.flatMap { enabled =>
        if (enabled) {
          for (localCtx <- localLogContext.currentLogContext)
            yield underlying.error(localCtx)(message)
        } else F.unit
      }
    def error(t: Throwable)(message: => String): F[Unit] =
      underlying.isErrorEnabled.flatMap { enabled =>
        if (enabled) {
          for (localCtx <- localLogContext.currentLogContext)
            yield underlying.error(localCtx, t)(message)
        } else F.unit
      }
    def error(ctx: Map[String, String])(msg: => String): F[Unit] =
      underlying.isErrorEnabled.flatMap { enabled =>
        if (enabled) {
          for (localCtx <- localLogContext.currentLogContext)
            yield underlying.error(localCtx ++ ctx)(msg)
        } else F.unit
      }
    def error(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
      underlying.isErrorEnabled.flatMap { enabled =>
        if (enabled) {
          for (localCtx <- localLogContext.currentLogContext)
            yield underlying.error(localCtx ++ ctx, t)(msg)
        } else F.unit
      }

    def warn(message: => String): F[Unit] =
      underlying.isErrorEnabled.flatMap { enabled =>
        if (enabled) {
          for (localCtx <- localLogContext.currentLogContext)
            yield underlying.warn(localCtx)(message)
        } else F.unit
      }
    def warn(t: Throwable)(message: => String): F[Unit] =
      underlying.isErrorEnabled.flatMap { enabled =>
        if (enabled) {
          for (localCtx <- localLogContext.currentLogContext)
            yield underlying.warn(localCtx, t)(message)
        } else F.unit
      }
    def warn(ctx: Map[String, String])(msg: => String): F[Unit] =
      underlying.isErrorEnabled.flatMap { enabled =>
        if (enabled) {
          for (localCtx <- localLogContext.currentLogContext)
            yield underlying.warn(localCtx ++ ctx)(msg)
        } else F.unit
      }
    def warn(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
      underlying.isErrorEnabled.flatMap { enabled =>
        if (enabled) {
          for (localCtx <- localLogContext.currentLogContext)
            yield underlying.warn(localCtx ++ ctx, t)(msg)
        } else F.unit
      }

    def info(message: => String): F[Unit] =
      underlying.isErrorEnabled.flatMap { enabled =>
        if (enabled) {
          for (localCtx <- localLogContext.currentLogContext)
            yield underlying.info(localCtx)(message)
        } else F.unit
      }
    def info(t: Throwable)(message: => String): F[Unit] =
      underlying.isErrorEnabled.flatMap { enabled =>
        if (enabled) {
          for (localCtx <- localLogContext.currentLogContext)
            yield underlying.info(localCtx, t)(message)
        } else F.unit
      }
    def info(ctx: Map[String, String])(msg: => String): F[Unit] =
      underlying.isErrorEnabled.flatMap { enabled =>
        if (enabled) {
          for (localCtx <- localLogContext.currentLogContext)
            yield underlying.info(localCtx ++ ctx)(msg)
        } else F.unit
      }
    def info(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
      underlying.isErrorEnabled.flatMap { enabled =>
        if (enabled) {
          for (localCtx <- localLogContext.currentLogContext)
            yield underlying.info(localCtx ++ ctx, t)(msg)
        } else F.unit
      }

    def debug(message: => String): F[Unit] =
      underlying.isErrorEnabled.flatMap { enabled =>
        if (enabled) {
          for (localCtx <- localLogContext.currentLogContext)
            yield underlying.debug(localCtx)(message)
        } else F.unit
      }
    def debug(t: Throwable)(message: => String): F[Unit] =
      underlying.isErrorEnabled.flatMap { enabled =>
        if (enabled) {
          for (localCtx <- localLogContext.currentLogContext)
            yield underlying.debug(localCtx, t)(message)
        } else F.unit
      }
    def debug(ctx: Map[String, String])(msg: => String): F[Unit] =
      underlying.isErrorEnabled.flatMap { enabled =>
        if (enabled) {
          for (localCtx <- localLogContext.currentLogContext)
            yield underlying.debug(localCtx ++ ctx)(msg)
        } else F.unit
      }
    def debug(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
      underlying.isErrorEnabled.flatMap { enabled =>
        if (enabled) {
          for (localCtx <- localLogContext.currentLogContext)
            yield underlying.debug(localCtx ++ ctx, t)(msg)
        } else F.unit
      }

    def trace(message: => String): F[Unit] =
      underlying.isErrorEnabled.flatMap { enabled =>
        if (enabled) {
          for (localCtx <- localLogContext.currentLogContext)
            yield underlying.trace(localCtx)(message)
        } else F.unit
      }
    def trace(t: Throwable)(message: => String): F[Unit] =
      underlying.isErrorEnabled.flatMap { enabled =>
        if (enabled) {
          for (localCtx <- localLogContext.currentLogContext)
            yield underlying.trace(localCtx, t)(message)
        } else F.unit
      }
    def trace(ctx: Map[String, String])(msg: => String): F[Unit] =
      underlying.isErrorEnabled.flatMap { enabled =>
        if (enabled) {
          for (localCtx <- localLogContext.currentLogContext)
            yield underlying.trace(localCtx ++ ctx)(msg)
        } else F.unit
      }
    def trace(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
      underlying.isErrorEnabled.flatMap { enabled =>
        if (enabled) {
          for (localCtx <- localLogContext.currentLogContext)
            yield underlying.trace(localCtx ++ ctx, t)(msg)
        } else F.unit
      }
  }

  def apply[F[_]: Monad](
      localLogContext: LocalLogContext[F],
      underlying: SelfAwareStructuredLogger[F]
  ): LocalLogger[F] =
    new Impl(localLogContext, underlying)

  def fromLocal[F[_]: Monad](
      underlying: SelfAwareStructuredLogger[F]
  )(implicit localCtx: Local[F, Map[String, String]]): LocalLogger[F] =
    apply(LocalLogContext.fromLocal, underlying)
}
