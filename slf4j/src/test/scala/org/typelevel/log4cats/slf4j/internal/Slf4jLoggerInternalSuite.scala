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

package org.typelevel.log4cats.slf4j
package internal

import cats.effect.IO
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
}
