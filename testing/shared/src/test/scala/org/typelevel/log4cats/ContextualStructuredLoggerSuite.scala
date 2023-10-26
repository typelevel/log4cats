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

import cats.arrow.FunctionK
import cats.data.{Kleisli, ReaderT}
import cats.effect._
import cats.mtl.Local
import cats.mtl.syntax.local._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.{~>, FlatMap}
import munit._
import org.typelevel.log4cats.mtl.ToContext
import org.typelevel.log4cats.mtl.syntax._
import org.typelevel.log4cats.testing.StructuredTestingLogger

class ContextualStructuredLoggerSuite extends CatsEffectSuite {
  import StructuredTestingLogger.{ERROR, INFO}

  test("Kleisli - capture contextual information") {
    test[IO, Kleisli[IO, LogContext, *]](Kleisli.liftK).run(LogContext("external"))
  }

  test("ReaderT - capture contextual information") {
    test[IO, ReaderT[IO, LogContext, *]](ReaderT.liftK).run(LogContext("external"))
  }

  test("IOLocal - capture contextual information") {
    IOLocal(LogContext("external")).flatMap { implicit ioLocal =>
      test[IO, IO](FunctionK.id)
    }
  }

  private def test[F[_]: Sync, G[_]: FlatMap](
      fg: F ~> G
  )(implicit local: Local[G, LogContext]): G[Unit] = {
    val logger: StructuredTestingLogger[F] =
      StructuredTestingLogger.impl()

    val contextual: StructuredLogger[G] =
      logger.mapK(fg).contextual

    val io: G[Unit] =
      for {
        _ <- contextual.info("simple message")
        _ <- contextual.info(Map("a" -> "b"))("with context")
        _ <- contextual.info(Map("c" -> "d"))("inner with context").scope(LogContext("inner"))
        _ <- contextual.error("the error one")
      } yield ()

    val expected = Vector(
      INFO("simple message", None, Map("log_id" -> "global")),
      INFO("with context", None, Map("log_id" -> "global", "a" -> "b")),
      INFO("inner with context", None, Map("log_id" -> "inner", "c" -> "d")),
      ERROR("the error one", None, Map("log_id" -> "global"))
    )

    for {
      _ <- io.scope(LogContext("global"))
      logged <- fg(logger.logged)
    } yield assertEquals(logged, expected)
  }

  case class LogContext(logId: String)

  implicit val toLogContext: ToContext[LogContext] =
    ctx => Map("log_id" -> ctx.logId)

  // We hope this instance is moved into Cats Effect.
  // copy-pasted from otel4s
  implicit def localForIoLocal[F[_]: MonadCancelThrow: LiftIO, E](implicit
      ioLocal: IOLocal[E]
  ): Local[F, E] =
    new Local[F, E] {
      def applicative =
        MonadCancelThrow[F]

      def ask[E2 >: E] =
        MonadCancelThrow[F].widen[E, E2](ioLocal.get.to[F])

      def local[A](fa: F[A])(f: E => E): F[A] =
        MonadCancelThrow[F]
          .bracket(ioLocal.modify(e => (f(e), e)).to[F])(_ => fa)(ioLocal.set(_).to[F])
    }

}
