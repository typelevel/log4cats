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

import cats.effect.IO
import cats.mtl.Ask
import munit.CatsEffectSuite

class LocalLogContextTest extends CatsEffectSuite {
  private[this] val baseLLC: IO[LocalLogContext[IO]] =
    IO.local(Map("base" -> "init"))
      .map(implicit iol => LocalLogContext.fromLocal)

  private[this] def ctxTest(name: String)(body: LocalLogContext[IO] => IO[?]): Unit =
    test(name)(baseLLC.flatMap(body))

  private[this] def ask(name: String): Ask[IO, Map[String, String]] =
    Ask.const(Map("shared" -> name, name -> "1"))

  private[this] val a = ask("a")
  private[this] val b = ask("b")

  ctxTest("retains value in backing Local") { llc =>
    for (ctx <- llc.currentLogContext)
      yield assertEquals(ctx, Map("base" -> "init"))
  }

  ctxTest("high priority ask overrides base") { base =>
    val llc = base.withHighPriorityAskedContext(a)
    for {
      ctx <- llc.withAddedContext("base" -> "new", "shared" -> "base") {
        llc.currentLogContext
      }
    } yield assertEquals(ctx, Map("base" -> "new", "shared" -> "a", "a" -> "1"))
  }

  ctxTest("base overrides low priority ask") { base =>
    val llc = base.withLowPriorityAskedContext(a)
    for {
      ctx <- llc.withAddedContext("base" -> "new", "shared" -> "base") {
        llc.currentLogContext
      }
    } yield assertEquals(ctx, Map("base" -> "new", "shared" -> "base", "a" -> "1"))
  }

  ctxTest("high priority ask overrides base and low priority ask") { base =>
    val llc = base
      .withLowPriorityAskedContext(a)
      .withHighPriorityAskedContext(b)
    for {
      ctx <- llc.withAddedContext("base" -> "new", "shared" -> "base") {
        llc.currentLogContext
      }
    } yield assertEquals(
      ctx,
      Map("base" -> "new", "shared" -> "b", "a" -> "1", "b" -> "1")
    )
  }

  ctxTest("second high priority ask overrides first high priority ask") { base =>
    val llc = base
      .withHighPriorityAskedContext(a)
      .withHighPriorityAskedContext(b)
    for {
      ctx <- llc.withAddedContext("base" -> "new", "shared" -> "base") {
        llc.currentLogContext
      }
    } yield assertEquals(
      ctx,
      Map("base" -> "new", "shared" -> "b", "a" -> "1", "b" -> "1")
    )
  }

  ctxTest("first low priority ask overrides second low priority ask") { base =>
    val llc = base
      .withLowPriorityAskedContext(a)
      .withLowPriorityAskedContext(b)
    for {
      ctx <- llc.withAddedContext(Map("base" -> "new")) {
        llc.currentLogContext
      }
    } yield assertEquals(
      ctx,
      Map("base" -> "new", "shared" -> "a", "a" -> "1", "b" -> "1")
    )
  }
}
