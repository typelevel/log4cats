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

import cats.arrow.FunctionK
import cats.effect.{IO, Resource, SyncIO}
import cats.syntax.all.*

import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import org.slf4j.MDC
import munit.CatsEffectSuite
import org.typelevel.log4cats.extras.DeferredLogMessage

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContextExecutorService
import scala.jdk.CollectionConverters.*
import scala.util.control.NoStackTrace

class Slf4jLoggerInternalSuite extends CatsEffectSuite {

  // Fixes a flaky test issue that may or may not be a bug
  // See: https://github.com/typelevel/log4cats/issues/851
  override def beforeEach(context: BeforeEach): Unit = {
    super.beforeEach(context)
    MDC.clear()
  }

  object dirtyStuff {

    def namedSingleThreadExecutionContext(name: String): ExecutionContextExecutorService =
      ExecutionContext.fromExecutorService(
        Executors.newSingleThreadExecutor(new ThreadFactory() {
          def newThread(r: Runnable): Thread = new Thread(r, name)
        })
      )

    def killThreads(threads: List[ExecutionContextExecutorService]): Unit = threads.foreach {
      thread =>
        try thread.shutdownNow()
        catch {
          case e: Throwable =>
            Console.err.println("Couldn't shutdown thread")
            e.printStackTrace()
        }
    }
  }

  def testLoggerFixture(
      traceEnabled: Boolean = true,
      debugEnabled: Boolean = true,
      infoEnabled: Boolean = true,
      warnEnabled: Boolean = true,
      errorEnabled: Boolean = true
  ): SyncIO[FunFixture[JTestLogger]] =
    ResourceFunFixture(
      Resource.eval(
        IO(
          new JTestLogger(
            "Test Logger",
            traceEnabled,
            debugEnabled,
            infoEnabled,
            warnEnabled,
            errorEnabled
          )
        )
      )
    )

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

  testLoggerFixture().test("Slf4jLoggerInternal correctly sets the MDC") { testLogger =>
    Slf4jLogger
      .getLoggerFromSlf4j[IO](testLogger)
      .info(Map("foo" -> "bar"))("A log went here") >>
      IO(testLogger.logs())
        .map(_.asScala.toList)
        .assertEquals(
          List(
            DeferredLogMessage.info(Map("foo" -> "bar"), none, () => "A log went here")
          )
        )
  }

  testLoggerFixture(
    traceEnabled = false,
    debugEnabled = false,
    infoEnabled = false,
    warnEnabled = false,
    errorEnabled = false
  ).test("Slf4jLoggerInternal is suitably lazy") { testLogger =>
    def die(): String = throw new IllegalStateException()
    val slf4jLogger = Slf4jLogger.getLoggerFromSlf4j[IO](testLogger)
    val dummyThrowable = new NoSuchElementException() with NoStackTrace
    val ctx = Map("foo" -> "bar")
    // If these are lazy the way they need to be, the message won't be evaluated until
    // after the log level has been checked
    slf4jLogger.trace(die()).assert >>
      slf4jLogger.debug(die()).assert >>
      slf4jLogger.info(die()).assert >>
      slf4jLogger.warn(die()).assert >>
      slf4jLogger.error(die()).assert >>
      slf4jLogger.trace(dummyThrowable)(die()).assert >>
      slf4jLogger.debug(dummyThrowable)(die()).assert >>
      slf4jLogger.info(dummyThrowable)(die()).assert >>
      slf4jLogger.warn(dummyThrowable)(die()).assert >>
      slf4jLogger.error(dummyThrowable)(die()).assert >>
      slf4jLogger.trace(ctx)(die()).assert >>
      slf4jLogger.debug(ctx)(die()).assert >>
      slf4jLogger.info(ctx)(die()).assert >>
      slf4jLogger.warn(ctx)(die()).assert >>
      slf4jLogger.error(ctx)(die()).assert >>
      slf4jLogger.trace(ctx, dummyThrowable)(die()).assert >>
      slf4jLogger.debug(ctx, dummyThrowable)(die()).assert >>
      slf4jLogger.info(ctx, dummyThrowable)(die()).assert >>
      slf4jLogger.warn(ctx, dummyThrowable)(die()).assert >>
      slf4jLogger.error(ctx, dummyThrowable)(die()).assert
  }

  testLoggerFixture(
    traceEnabled = false,
    debugEnabled = false,
    infoEnabled = false,
    warnEnabled = false,
    errorEnabled = false
  ).test("Slf4jLoggerInternal.mapK is still lazy") { testLogger =>
    def die(): String = throw new IllegalStateException()
    val slf4jLogger = Slf4jLogger.getLoggerFromSlf4j[IO](testLogger).mapK[IO](FunctionK.id)
    val dummyThrowable = new NoSuchElementException() with NoStackTrace
    val ctx = Map("foo" -> "bar")
    // If these are lazy the way they need to be, the message won't be evaluated until
    // after the log level has been checked
    slf4jLogger.trace(die()).assert >>
      slf4jLogger.debug(die()).assert >>
      slf4jLogger.info(die()).assert >>
      slf4jLogger.warn(die()).assert >>
      slf4jLogger.error(die()).assert >>
      slf4jLogger.trace(dummyThrowable)(die()).assert >>
      slf4jLogger.debug(dummyThrowable)(die()).assert >>
      slf4jLogger.info(dummyThrowable)(die()).assert >>
      slf4jLogger.warn(dummyThrowable)(die()).assert >>
      slf4jLogger.error(dummyThrowable)(die()).assert >>
      slf4jLogger.trace(ctx)(die()).assert >>
      slf4jLogger.debug(ctx)(die()).assert >>
      slf4jLogger.info(ctx)(die()).assert >>
      slf4jLogger.warn(ctx)(die()).assert >>
      slf4jLogger.error(ctx)(die()).assert >>
      slf4jLogger.trace(ctx, dummyThrowable)(die()).assert >>
      slf4jLogger.debug(ctx, dummyThrowable)(die()).assert >>
      slf4jLogger.info(ctx, dummyThrowable)(die()).assert >>
      slf4jLogger.warn(ctx, dummyThrowable)(die()).assert >>
      slf4jLogger.error(ctx, dummyThrowable)(die()).assert
  }

  testLoggerFixture(
    traceEnabled = false,
    debugEnabled = false,
    infoEnabled = false,
    warnEnabled = false,
    errorEnabled = false
  ).test("Slf4jLoggerInternal.withModifiedString is still lazy") { testLogger =>
    def die(): String = throw new IllegalStateException()
    val slf4jLogger =
      Slf4jLogger.getLoggerFromSlf4j[IO](testLogger).withModifiedString(_.toUpperCase)
    val dummyThrowable = new NoSuchElementException() with NoStackTrace
    val ctx = Map("foo" -> "bar")
    // If these are lazy the way they need to be, the message won't be evaluated until
    // after the log level has been checked
    slf4jLogger.trace(die()).assert >>
      slf4jLogger.debug(die()).assert >>
      slf4jLogger.info(die()).assert >>
      slf4jLogger.warn(die()).assert >>
      slf4jLogger.error(die()).assert >>
      slf4jLogger.trace(dummyThrowable)(die()).assert >>
      slf4jLogger.debug(dummyThrowable)(die()).assert >>
      slf4jLogger.info(dummyThrowable)(die()).assert >>
      slf4jLogger.warn(dummyThrowable)(die()).assert >>
      slf4jLogger.error(dummyThrowable)(die()).assert >>
      slf4jLogger.trace(ctx)(die()).assert >>
      slf4jLogger.debug(ctx)(die()).assert >>
      slf4jLogger.info(ctx)(die()).assert >>
      slf4jLogger.warn(ctx)(die()).assert >>
      slf4jLogger.error(ctx)(die()).assert >>
      slf4jLogger.trace(ctx, dummyThrowable)(die()).assert >>
      slf4jLogger.debug(ctx, dummyThrowable)(die()).assert >>
      slf4jLogger.info(ctx, dummyThrowable)(die()).assert >>
      slf4jLogger.warn(ctx, dummyThrowable)(die()).assert >>
      slf4jLogger.error(ctx, dummyThrowable)(die()).assert
  }

  testLoggerFixture(
    traceEnabled = false,
    debugEnabled = false,
    infoEnabled = false,
    warnEnabled = false,
    errorEnabled = false
  ).test("Slf4jLoggerInternal.addContext is still lazy") { testLogger =>
    def die(): String = throw new IllegalStateException()
    val slf4jLogger = Slf4jLogger.getLoggerFromSlf4j[IO](testLogger).addContext(Map("bar" -> "foo"))
    val dummyThrowable = new NoSuchElementException() with NoStackTrace
    val ctx = Map("foo" -> "bar")
    // If these are lazy the way they need to be, the message won't be evaluated until
    // after the log level has been checked
    slf4jLogger.trace(die()).assert >>
      slf4jLogger.debug(die()).assert >>
      slf4jLogger.info(die()).assert >>
      slf4jLogger.warn(die()).assert >>
      slf4jLogger.error(die()).assert >>
      slf4jLogger.trace(dummyThrowable)(die()).assert >>
      slf4jLogger.debug(dummyThrowable)(die()).assert >>
      slf4jLogger.info(dummyThrowable)(die()).assert >>
      slf4jLogger.warn(dummyThrowable)(die()).assert >>
      slf4jLogger.error(dummyThrowable)(die()).assert >>
      slf4jLogger.trace(ctx)(die()).assert >>
      slf4jLogger.debug(ctx)(die()).assert >>
      slf4jLogger.info(ctx)(die()).assert >>
      slf4jLogger.warn(ctx)(die()).assert >>
      slf4jLogger.error(ctx)(die()).assert >>
      slf4jLogger.trace(ctx, dummyThrowable)(die()).assert >>
      slf4jLogger.debug(ctx, dummyThrowable)(die()).assert >>
      slf4jLogger.info(ctx, dummyThrowable)(die()).assert >>
      slf4jLogger.warn(ctx, dummyThrowable)(die()).assert >>
      slf4jLogger.error(ctx, dummyThrowable)(die()).assert
  }

  testLoggerFixture().test("Slf4jLoggerInternal gets the dispatching right") { testLogger =>
    val slf4jLogger = Slf4jLogger.getLoggerFromSlf4j[IO](testLogger)
    def throwable(tag: String) = new NoSuchElementException(tag) with NoStackTrace
    def ctx(tag: String) = Map("tag" -> tag)
    // If these are lazy the way they need to be, the message won't be evaluated until
    // after the log level has been checked
    slf4jLogger.trace("trace(msg)").assert >>
      slf4jLogger.debug("debug(msg)").assert >>
      slf4jLogger.info("info(msg)").assert >>
      slf4jLogger.warn("warn(msg)").assert >>
      slf4jLogger.error("error(msg)").assert >>
      IO(testLogger.logs().asScala.toList).assertEquals(
        List(
          DeferredLogMessage.trace(Map.empty, none, () => "trace(msg)"),
          DeferredLogMessage.debug(Map.empty, none, () => "debug(msg)"),
          DeferredLogMessage.info(Map.empty, none, () => "info(msg)"),
          DeferredLogMessage.warn(Map.empty, none, () => "warn(msg)"),
          DeferredLogMessage.error(Map.empty, none, () => "error(msg)")
        )
      ) >>
      IO(testLogger.reset()) >>
      slf4jLogger.trace(throwable("trace(t)(msg)"))("trace(t)(msg)").assert >>
      slf4jLogger.debug(throwable("debug(t)(msg)"))("debug(t)(msg)").assert >>
      slf4jLogger.info(throwable("info(t)(msg)"))("info(t)(msg)").assert >>
      slf4jLogger.warn(throwable("warn(t)(msg)"))("warn(t)(msg)").assert >>
      slf4jLogger.error(throwable("error(t)(msg)"))("error(t)(msg)").assert >>
      IO(testLogger.logs().asScala.toList).assertEquals(
        List(
          DeferredLogMessage
            .trace(Map.empty, throwable("trace(t)(msg)").some, () => "trace(t)(msg)"),
          DeferredLogMessage
            .debug(Map.empty, throwable("debug(t)(msg)").some, () => "debug(t)(msg)"),
          DeferredLogMessage.info(Map.empty, throwable("info(t)(msg)").some, () => "info(t)(msg)"),
          DeferredLogMessage.warn(Map.empty, throwable("warn(t)(msg)").some, () => "warn(t)(msg)"),
          DeferredLogMessage.error(
            Map.empty,
            throwable("error(t)(msg)").some,
            () => "error(t)(msg)"
          )
        )
      ) >>
      IO(testLogger.reset()) >>
      slf4jLogger.trace(ctx("trace(ctx)(msg)"))("trace(ctx)(msg)").assert >>
      slf4jLogger.debug(ctx("debug(ctx)(msg)"))("debug(ctx)(msg)").assert >>
      slf4jLogger.info(ctx("info(ctx)(msg)"))("info(ctx)(msg)").assert >>
      slf4jLogger.warn(ctx("warn(ctx)(msg)"))("warn(ctx)(msg)").assert >>
      slf4jLogger.error(ctx("error(ctx)(msg)"))("error(ctx)(msg)").assert >>
      IO(testLogger.logs().asScala.toList).assertEquals(
        List(
          DeferredLogMessage.trace(ctx("trace(ctx)(msg)"), none, () => "trace(ctx)(msg)"),
          DeferredLogMessage.debug(ctx("debug(ctx)(msg)"), none, () => "debug(ctx)(msg)"),
          DeferredLogMessage.info(ctx("info(ctx)(msg)"), none, () => "info(ctx)(msg)"),
          DeferredLogMessage.warn(ctx("warn(ctx)(msg)"), none, () => "warn(ctx)(msg)"),
          DeferredLogMessage.error(ctx("error(ctx)(msg)"), none, () => "error(ctx)(msg)")
        )
      ) >>
      IO(testLogger.reset()) >>
      slf4jLogger
        .trace(ctx("trace(ctx, t)(msg)"), throwable("trace(ctx, t)(msg)"))("trace(ctx, t)(msg)")
        .assert >>
      slf4jLogger
        .debug(ctx("debug(ctx, t)(msg)"), throwable("debug(ctx, t)(msg)"))("debug(ctx, t)(msg)")
        .assert >>
      slf4jLogger
        .info(ctx("info(ctx, t)(msg)"), throwable("info(ctx, t)(msg)"))("info(ctx, t)(msg)")
        .assert >>
      slf4jLogger
        .warn(ctx("warn(ctx, t)(msg)"), throwable("warn(ctx, t)(msg)"))("warn(ctx, t)(msg)")
        .assert >>
      slf4jLogger
        .error(ctx("error(ctx, t)(msg)"), throwable("error(ctx, t)(msg)"))("error(ctx, t)(msg)")
        .assert >>
      IO(testLogger.logs().asScala.toList).assertEquals(
        List(
          DeferredLogMessage.trace(
            ctx("trace(ctx, t)(msg)"),
            throwable("trace(ctx, t)(msg)").some,
            () => "trace(ctx, t)(msg)"
          ),
          DeferredLogMessage.debug(
            ctx("debug(ctx, t)(msg)"),
            throwable("debug(ctx, t)(msg)").some,
            () => "debug(ctx, t)(msg)"
          ),
          DeferredLogMessage.info(
            ctx("info(ctx, t)(msg)"),
            throwable("info(ctx, t)(msg)").some,
            () => "info(ctx, t)(msg)"
          ),
          DeferredLogMessage.warn(
            ctx("warn(ctx, t)(msg)"),
            throwable("warn(ctx, t)(msg)").some,
            () => "warn(ctx, t)(msg)"
          ),
          DeferredLogMessage.error(
            ctx("error(ctx, t)(msg)"),
            throwable("error(ctx, t)(msg)").some,
            () => "error(ctx, t)(msg)"
          )
        )
      )
  }
}
