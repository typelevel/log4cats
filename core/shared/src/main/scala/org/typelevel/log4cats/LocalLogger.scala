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

import cats.mtl.{LiftKind, Local}
import cats.syntax.flatMap._
import cats.{~>, Monad, Show}

/**
 * A logger with [[cats.mtl.Local `Local`]] semantics.
 *
 * @see
 *   [[withAddedContext]]
 */
sealed trait LocalLogger[F[_]] extends SelfAwareLogger[F] {

  /**
   * Modifies the given effect to have the provided context stored [[cats.mtl.Local locally]].
   *
   * Context added using this method is available to all loggers created by this logger's
   * [[LocalLoggerFactory parent factory]].
   */
  def withAddedContext[A](ctx: Map[String, String])(fa: F[A]): F[A]

  /**
   * Modifies the given effect to have the provided context stored [[cats.mtl.Local locally]].
   *
   * Context added using this method is available to all loggers created by this logger's
   * [[LocalLoggerFactory parent factory]].
   */
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

  @deprecated("use `liftTo` instead", since = "log4cats 2.8.0")
  override def mapK[G[_]](fk: F ~> G): SelfAwareLogger[G] = super.mapK(fk)

  /** Lifts this logger's context from `F` to `G`. */
  def liftTo[G[_]](implicit lift: LiftKind[F, G], G: Monad[G]): LocalLogger[G]

  protected[this] def withModifiedStringImpl(f: String => String): LocalLogger[F]

  override def withModifiedString(f: String => String): LocalLogger[F] =
    withModifiedStringImpl(f)

  /**
   * A view of this logger as a [[`StructuredLogger`]], to support gradual migration away from
   * `StructuredLogger`. Log context added using this `LocalLogger` or its
   * [[LocalLoggerFactory parent factory]] will be included in log messages created by
   * `StructuredLogger`s returned by this method, regardless of the scope in which this method was
   * called.
   */
  @deprecated(
    "`StructuredLogger` is cumbersome and lacks `cats.mtl.Local` semantics",
    since = "log4cats 2.8.0"
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

    @deprecated("use `liftTo` instead", since = "log4cats 2.8.0")
    override def mapK[G[_]](fk: F ~> G): SelfAwareStructuredLogger[G] =
      super.mapK(fk)
    def liftTo[G[_]](implicit lift: LiftKind[F, G], G: Monad[G]): LocalLogger[G] =
      new Impl(localLogContext.liftTo[G], underlying.mapK(lift))
    override def withModifiedStringImpl(f: String => String): Impl[F] =
      new Impl(localLogContext, underlying.withModifiedString(f))
    override def withModifiedString(f: String => String): Impl[F] =
      withModifiedStringImpl(f)

    @deprecated(
      "`StructuredLogger` is cumbersome and lacks `cats.mtl.Local` semantics",
      since = "log4cats 2.8.0"
    )
    def asStructuredLogger: SelfAwareStructuredLogger[F] = this

    def error(message: => String): F[Unit] =
      F.ifM(underlying.isErrorEnabled)(
        localLogContext.currentLogContext
          .flatMap(underlying.error(_)(message)),
        F.unit
      )
    def error(t: Throwable)(message: => String): F[Unit] =
      F.ifM(underlying.isErrorEnabled)(
        localLogContext.currentLogContext
          .flatMap(underlying.error(_, t)(message)),
        F.unit
      )
    def error(ctx: Map[String, String])(msg: => String): F[Unit] =
      F.ifM(underlying.isErrorEnabled)(
        localLogContext.currentLogContext
          .flatMap(localCtx => underlying.error(localCtx ++ ctx)(msg)),
        F.unit
      )
    def error(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
      F.ifM(underlying.isErrorEnabled)(
        localLogContext.currentLogContext
          .flatMap(localCtx => underlying.error(localCtx ++ ctx, t)(msg)),
        F.unit
      )

    def warn(message: => String): F[Unit] =
      F.ifM(underlying.isWarnEnabled)(
        localLogContext.currentLogContext
          .flatMap(underlying.warn(_)(message)),
        F.unit
      )
    def warn(t: Throwable)(message: => String): F[Unit] =
      F.ifM(underlying.isWarnEnabled)(
        localLogContext.currentLogContext
          .flatMap(underlying.warn(_, t)(message)),
        F.unit
      )
    def warn(ctx: Map[String, String])(msg: => String): F[Unit] =
      F.ifM(underlying.isWarnEnabled)(
        localLogContext.currentLogContext
          .flatMap(localCtx => underlying.warn(localCtx ++ ctx)(msg)),
        F.unit
      )
    def warn(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
      F.ifM(underlying.isWarnEnabled)(
        localLogContext.currentLogContext
          .flatMap(localCtx => underlying.warn(localCtx ++ ctx, t)(msg)),
        F.unit
      )

    def info(message: => String): F[Unit] =
      F.ifM(underlying.isInfoEnabled)(
        localLogContext.currentLogContext
          .flatMap(underlying.info(_)(message)),
        F.unit
      )
    def info(t: Throwable)(message: => String): F[Unit] =
      F.ifM(underlying.isInfoEnabled)(
        localLogContext.currentLogContext
          .flatMap(underlying.info(_, t)(message)),
        F.unit
      )
    def info(ctx: Map[String, String])(msg: => String): F[Unit] =
      F.ifM(underlying.isInfoEnabled)(
        localLogContext.currentLogContext
          .flatMap(localCtx => underlying.info(localCtx ++ ctx)(msg)),
        F.unit
      )
    def info(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
      F.ifM(underlying.isInfoEnabled)(
        localLogContext.currentLogContext
          .flatMap(localCtx => underlying.info(localCtx ++ ctx, t)(msg)),
        F.unit
      )

    def debug(message: => String): F[Unit] =
      F.ifM(underlying.isDebugEnabled)(
        localLogContext.currentLogContext
          .flatMap(underlying.debug(_)(message)),
        F.unit
      )
    def debug(t: Throwable)(message: => String): F[Unit] =
      F.ifM(underlying.isDebugEnabled)(
        localLogContext.currentLogContext
          .flatMap(underlying.debug(_, t)(message)),
        F.unit
      )
    def debug(ctx: Map[String, String])(msg: => String): F[Unit] =
      F.ifM(underlying.isDebugEnabled)(
        localLogContext.currentLogContext
          .flatMap(localCtx => underlying.debug(localCtx ++ ctx)(msg)),
        F.unit
      )
    def debug(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
      F.ifM(underlying.isDebugEnabled)(
        localLogContext.currentLogContext
          .flatMap(localCtx => underlying.debug(localCtx ++ ctx, t)(msg)),
        F.unit
      )

    def trace(message: => String): F[Unit] =
      F.ifM(underlying.isTraceEnabled)(
        localLogContext.currentLogContext
          .flatMap(underlying.trace(_)(message)),
        F.unit
      )
    def trace(t: Throwable)(message: => String): F[Unit] =
      F.ifM(underlying.isTraceEnabled)(
        localLogContext.currentLogContext
          .flatMap(underlying.trace(_, t)(message)),
        F.unit
      )
    def trace(ctx: Map[String, String])(msg: => String): F[Unit] =
      F.ifM(underlying.isTraceEnabled)(
        localLogContext.currentLogContext
          .flatMap(localCtx => underlying.trace(localCtx ++ ctx)(msg)),
        F.unit
      )
    def trace(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
      F.ifM(underlying.isTraceEnabled)(
        localLogContext.currentLogContext
          .flatMap(localCtx => underlying.trace(localCtx ++ ctx, t)(msg)),
        F.unit
      )
  }

  /**
   * This method should only be used when a [[`LoggerFactory`]] is not available; when possible,
   * create a [[`LocalLoggerFactory`]] and use that to create `LocalLogger`s.
   *
   * @return
   *   a [[cats.mtl.Local local]] logger backed by the given [[`LocalLogContext`]] and
   *   [[`LoggerFactory`]]
   */
  def apply[F[_]: Monad](
      localLogContext: LocalLogContext[F],
      underlying: SelfAwareStructuredLogger[F]
  ): LocalLogger[F] =
    new Impl(localLogContext, underlying)

  /**
   * This method should only be used when a [[`LoggerFactory`]] is not available; when possible,
   * create a [[`LocalLoggerFactory`]] and use that to create `LocalLogger`s.
   *
   * @return
   *   a local logger backed by the given [[`SelfAwareStructuredLogger`]] and implicit
   *   [[cats.mtl.Local `Local`]]
   */
  def fromLocal[F[_]: Monad](
      underlying: SelfAwareStructuredLogger[F]
  )(implicit localCtx: Local[F, Map[String, String]]): LocalLogger[F] =
    apply(LocalLogContext.fromLocal, underlying)
}
