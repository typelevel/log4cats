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

import cats.syntax.all.*
import cats.effect.*
import munit.{CatsEffectSuite, ScalaCheckEffectSuite}
import org.scalacheck.*
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.effect.PropF
import org.typelevel.log4cats.testing.StructuredTestingLogger
import org.typelevel.log4cats.testing.StructuredTestingLogger.{
  DEBUG,
  ERROR,
  INFO,
  LogMessage,
  TRACE,
  WARN
}

class DelegatingLoggerSuite extends CatsEffectSuite with ScalaCheckEffectSuite {
  private val genLogMessage: Gen[LogMessage] =
    for {
      msg <- arbitrary[String]
      ex <- arbitrary[Option[Throwable]]
      ctx <- arbitrary[Map[String, String]]
      constructor <- Gen.oneOf[(String, Option[Throwable], Map[String, String]) => LogMessage](
        TRACE.apply _,
        DEBUG.apply _,
        INFO.apply _,
        WARN.apply _,
        ERROR.apply _
      )
    } yield constructor(msg, ex, ctx)

  private implicit val arbLogMessage: Arbitrary[LogMessage] = Arbitrary(genLogMessage)

  test("messages should be given to the underlying logger") {
    PropF.forAllF {
      (
          expectedCtx: Map[String, String],
          expectedStringModifier: String => String,
          events: Vector[LogMessage]
      ) =>
        for {
          underlying <- StructuredTestingLogger.ref[IO]()

          logger = new DelegatingLogger[IO](
            underlying
              .addContext(expectedCtx)
              .withModifiedString(expectedStringModifier)
              .pure[IO]
          )

          _ <- events.traverse {
            case TRACE(message, Some(ex), ctx) if ctx.isEmpty => logger.trace(ex)(message)
            case TRACE(message, None, ctx) if ctx.isEmpty => logger.trace(message)
            case TRACE(message, Some(ex), ctx) => logger.trace(ctx, ex)(message)
            case TRACE(message, None, ctx) => logger.trace(ctx)(message)

            case DEBUG(message, Some(ex), ctx) if ctx.isEmpty => logger.debug(ex)(message)
            case DEBUG(message, None, ctx) if ctx.isEmpty => logger.debug(message)
            case DEBUG(message, Some(ex), ctx) => logger.debug(ctx, ex)(message)
            case DEBUG(message, None, ctx) => logger.debug(ctx)(message)

            case INFO(message, Some(ex), ctx) if ctx.isEmpty => logger.info(ex)(message)
            case INFO(message, None, ctx) if ctx.isEmpty => logger.info(message)
            case INFO(message, Some(ex), ctx) => logger.info(ctx, ex)(message)
            case INFO(message, None, ctx) => logger.info(ctx)(message)

            case WARN(message, Some(ex), ctx) if ctx.isEmpty => logger.warn(ex)(message)
            case WARN(message, None, ctx) if ctx.isEmpty => logger.warn(message)
            case WARN(message, Some(ex), ctx) => logger.warn(ctx, ex)(message)
            case WARN(message, None, ctx) => logger.warn(ctx)(message)

            case ERROR(message, Some(ex), ctx) if ctx.isEmpty => logger.error(ex)(message)
            case ERROR(message, None, ctx) if ctx.isEmpty => logger.error(message)
            case ERROR(message, Some(ex), ctx) => logger.error(ctx, ex)(message)
            case ERROR(message, None, ctx) => logger.error(ctx)(message)
          }

          loggedEvents <- underlying.logged
          expectedEvents = events.map(
            _.mapCtx(expectedCtx ++ _).modifyString(expectedStringModifier)
          )
        } yield assertEquals(loggedEvents, expectedEvents)
    }
  }

  test("transformation effect is applied for each subsequent log event") {
    val indexes = (0 until 10).toVector
    for {
      countdown <- Ref[IO].of(indexes)
      expectedLogEvents = indexes.map(i => INFO("log event", None, Map("index" -> i.toString)))

      underlying <- StructuredTestingLogger.ref[IO]()

      transform =
        countdown.modify {
          case i +: tail => (tail, underlying.addContext(Map("index" -> i.toString)))
          case _ => throw new IllegalStateException("Countdown is empty")
        }

      logger = new DelegatingLogger[IO](transform)

      _ <- indexes.traverse_(_ => logger.info("log event"))

      loggedEvents <- underlying.logged
    } yield assertEquals(loggedEvents, expectedLogEvents)
  }
}
