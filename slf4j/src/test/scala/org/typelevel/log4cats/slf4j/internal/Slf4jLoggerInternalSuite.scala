/*
 * Copyright 2018 Christopher Davenport
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

package org.typelevel.log4cats.slf4j
package internal

import cats.effect.FiberIO
import cats.effect.IO
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import org.slf4j.MDC
import munit.CatsEffectSuite
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContextExecutorService

class Slf4jLoggerInternalSuite extends CatsEffectSuite {

  object dirtyStuff {

    def namedSingleThreadExecutionContext(name: String): ExecutionContextExecutorService =
      ExecutionContext.fromExecutorService(
        Executors.newSingleThreadExecutor(new ThreadFactory() {
          def newThread(r: Runnable): Thread = new Thread(r, name)
        })
      )

    def killThreads(threads: List[ExecutionContextExecutorService]) = threads.foreach { thread =>
      try thread.shutdownNow()
      catch {
        case e: Throwable =>
          Console.err.println("Couldn't shutdown thread")
          e.printStackTrace()
      }
    }
  }

  test("Slf4jLoggerInternal resets after logging") {
    val variable = "foo"
    val initial = "yellow"
    MDC.put(variable, initial)

    Slf4jLogger
      .getLogger[IO]
      .info(Map(variable -> "bar"))("A log went here")
      .as(MDC.get(variable))
      .assertEquals(initial)
  }

  test("Slf4jLoggerInternal resets on cancel") {
    val variable = "foo"
    val initial = "initial"

    import dirtyStuff._

    //logging happens here
    val loggerThread = namedSingleThreadExecutionContext("my-thread-1")

    //restoring context would run here if IO.bracket was used
    val finalizerThread = namedSingleThreadExecutionContext("my-thread-2")

    val mainThread = namedSingleThreadExecutionContext("main-thread")

    val startedLog = new CountDownLatch(1)
    val logCanceled = new CountDownLatch(1)
    val finishedLog = new CountDownLatch(1)

    val performLogging = Slf4jLogger
      .getLogger[IO]
      .info(Map(variable -> "modified")) {
        startedLog.countDown()
        logCanceled.await()
        finishedLog.countDown()
        "Heavy to compute value you're logging"
      }

    def performCancel[A](fiber: FiberIO[A]): IO[Unit] = {
      IO(startedLog.await()) *>
        fiber.cancel *>
        IO(logCanceled.countDown())
    }

    def getVariableOn(ec: ExecutionContext) =
      IO(MDC.get(variable)).evalOn(ec)

    (IO {
      IO(MDC.put(variable, initial)).evalOn(loggerThread)
      IO(MDC.put(variable, initial)).evalOn(finalizerThread)
    } *>
      performLogging
        .evalOn(loggerThread)
        .start
        .flatMap(
          performCancel(_)
            .evalOn(finalizerThread)
            .start
            .flatMap(_.join)
        ) *>
      IO(finishedLog.await()) *>
      getVariableOn(loggerThread).assertEquals(initial) *>
      getVariableOn(finalizerThread).assertEquals(initial) *>
      IO(MDC.get(variable)).assertEquals(null))
      .guarantee(IO(killThreads(List(loggerThread, finalizerThread, mainThread))))
  }
}
