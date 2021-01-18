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

import cats.effect.Fiber
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

    def runBlockingOn[A >: Null](a: => A)(ec: ExecutionContext): A = {
      val latch = new CountDownLatch(1)
      var ref: A = null

      ec.execute { () =>
        ref = a
        latch.countDown()
      }

      latch.await()
      ref
    }

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
    val mainInitial = "main initial"
    MDC.put(variable, mainInitial)

    import dirtyStuff._

    //logging happens here
    val loggerThread = namedSingleThreadExecutionContext("my-thread-1")

    //restoring context would run here if IO.bracket was used
    val finalizerThread = namedSingleThreadExecutionContext("my-thread-2")

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

    def performCancel[A](fiber: Fiber[IO, A]): IO[Unit] = {
      IO(startedLog.await()) *>
        fiber.cancel *>
        IO(logCanceled.countDown())
    }

    def getVariableOn(ec: ExecutionContext) =
      IO(runBlockingOn(MDC.get(variable))(ec))

    (IO {
      runBlockingOn(MDC.put(variable, initial))(loggerThread)
      runBlockingOn(MDC.put(variable, initial))(finalizerThread)
    } *>
      performLogging
        .start(IO.contextShift(loggerThread))
        .flatMap(
          performCancel(_)
            .start(IO.contextShift(finalizerThread))
            .flatMap(_.join)
        ) *>
      IO(finishedLog.await()) *>
      getVariableOn(loggerThread).assertEquals(initial) *>
      getVariableOn(finalizerThread).assertEquals(initial) *>
      IO(MDC.get(variable)).assertEquals(mainInitial))
      .guarantee(IO(killThreads(List(loggerThread, finalizerThread))))
  }
}
