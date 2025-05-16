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
import cats.effect.unsafe.IORuntime
import cats.effect.{IO, Resource, SyncIO}
import cats.syntax.all.*

import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import org.slf4j.MDC
import munit.{CatsEffectSuite, Location}
import org.typelevel.log4cats.extras.DeferredLogMessage
import org.typelevel.log4cats.slf4j.internal.JTestLogger.TestLogMessage

import java.util
import java.util.function
import java.util.function.{BiConsumer, BinaryOperator, Supplier}
import java.util.stream.Collector
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContextExecutorService
import scala.util.control.NoStackTrace

class Slf4jLoggerInternalSuite extends CatsEffectSuite {
  private val computeEC = ExecutionContext.fromExecutorService(
    Executors.newSingleThreadExecutor(),
    t => fail("Uncaught exception on compute thread", t)
  )

  private val blockingEC = ExecutionContext.fromExecutorService(
    Executors.newSingleThreadExecutor(),
    t => fail("Uncaught exception on blocking thread", t)
  )

  override implicit def munitIORuntime: IORuntime =
    IORuntime
      .builder()
      .setCompute(computeEC, () => computeEC.shutdown())
      .setBlocking(blockingEC, () => blockingEC.shutdown())
      .build()

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

  // Collections compat with Java is really annoying across the 2.12 / 2.13 boundary
  //
  // If you are reading this and support for 2.12 has been dropped, kindly rip this
  // out and call one of the helpers from scala.jdk.javaapi instead.
  private def toScalaList[A]: Collector[A, ListBuffer[A], List[A]] =
    new Collector[A, ListBuffer[A], List[A]] {
      override val characteristics: util.Set[Collector.Characteristics] =
        new util.HashSet[Collector.Characteristics]()

      override val supplier: Supplier[ListBuffer[A]] = () => new ListBuffer[A]

      override val accumulator: BiConsumer[ListBuffer[A], A] = (b, e) => b.append(e)

      override val combiner: BinaryOperator[ListBuffer[A]] = (a, b) => {
        a.appendAll(b)
        a
      }

      override val finisher: function.Function[ListBuffer[A], List[A]] = _.result()
    }

  private def toDeferredLogs(jl: java.util.List[TestLogMessage]): List[DeferredLogMessage] =
    jl.stream()
      .map[DeferredLogMessage] { tl =>
        val context =
          tl.context
            .entrySet()
            .stream()
            .map[(String, String)](e => e.getKey -> e.getValue)
            .collect(toScalaList)
            .toMap
        DeferredLogMessage(tl.logLevel, context, tl.throwableOpt, () => tl.message.get())
      }
      .collect(toScalaList[DeferredLogMessage])

  private val prepareMDC: IO[Unit] = IO.delay {
    MDC.clear()
    MDC.put("foo", "yellow")
  }

  private def validateMDC(implicit loc: Location): IO[Unit] =
    IO.delay(MDC.getCopyOfContextMap)
      .map(_.entrySet().stream().collect(toScalaList).map(e => e.getKey -> e.getValue).sorted)
      .assertEquals(
        List("foo" -> "yellow"),
        clue("MDC was not properly restored")
      )

  // These are literally just because I'm lazy
  private val throwable = new NoSuchElementException("error") with NoStackTrace
  private def die(): String = throw new IllegalStateException("dead")
  private def tag(t: String): Map[String, String] = Map("tag" -> t)

  test("Slf4jLoggerInternal resets after logging") {
    prepareMDC >>
      Slf4jLogger.getLogger[IO].info(Map("foo" -> "bar"))("A log went here") >>
      validateMDC
  }

  test("Slf4jLoggerInternal resets after exceptions") {
    prepareMDC >>
      Slf4jLogger
        .getLogger[IO]
        .info(Map("foo" -> "bar"))(die())
        .interceptMessage[IllegalStateException]("dead") >>
      validateMDC
  }

  testLoggerFixture().test("Slf4jLoggerInternal correctly sets the MDC") { testLogger =>
    prepareMDC >>
      Slf4jLogger.getLoggerFromSlf4j[IO](testLogger).info(Map("foo" -> "bar"))("A log went here") >>
      IO(testLogger.logs())
        .map(toDeferredLogs)
        .assertEquals(
          List(
            DeferredLogMessage.info(Map("foo" -> "bar"), none, () => "A log went here")
          )
        ) >>
      validateMDC
  }

  testLoggerFixture().test(
    "Slf4jLoggerInternal does not include values previously in the MDC in the log's context"
  ) { testLogger =>
    prepareMDC >>
      Slf4jLogger.getLoggerFromSlf4j[IO](testLogger).info(Map("bar" -> "baz"))("A log went here") >>
      IO(testLogger.logs())
        .map(toDeferredLogs)
        .assertEquals(
          List(DeferredLogMessage.info(Map("bar" -> "baz"), none, () => "A log went here")),
          clue("Context should not include foo->yellow")
        ) >>
      validateMDC
  }

  testLoggerFixture(
    traceEnabled = false,
    debugEnabled = false,
    infoEnabled = false,
    warnEnabled = false,
    errorEnabled = false
  ).test("Slf4jLoggerInternal is suitably lazy") { testLogger =>
    val slf4jLogger = Slf4jLogger.getLoggerFromSlf4j[IO](testLogger)
    val ctx = tag("lazy")
    // If these are lazy the way they need to be, the message won't be evaluated until
    // after the log level has been checked
    prepareMDC >>
      slf4jLogger.trace(die()).assert >>
      slf4jLogger.debug(die()).assert >>
      slf4jLogger.info(die()).assert >>
      slf4jLogger.warn(die()).assert >>
      slf4jLogger.error(die()).assert >>
      slf4jLogger.trace(throwable)(die()).assert >>
      slf4jLogger.debug(throwable)(die()).assert >>
      slf4jLogger.info(throwable)(die()).assert >>
      slf4jLogger.warn(throwable)(die()).assert >>
      slf4jLogger.error(throwable)(die()).assert >>
      slf4jLogger.trace(ctx)(die()).assert >>
      slf4jLogger.debug(ctx)(die()).assert >>
      slf4jLogger.info(ctx)(die()).assert >>
      slf4jLogger.warn(ctx)(die()).assert >>
      slf4jLogger.error(ctx)(die()).assert >>
      slf4jLogger.trace(ctx, throwable)(die()).assert >>
      slf4jLogger.debug(ctx, throwable)(die()).assert >>
      slf4jLogger.info(ctx, throwable)(die()).assert >>
      slf4jLogger.warn(ctx, throwable)(die()).assert >>
      slf4jLogger.error(ctx, throwable)(die()).assert >>
      validateMDC
  }

  testLoggerFixture(
    traceEnabled = false,
    debugEnabled = false,
    infoEnabled = false,
    warnEnabled = false,
    errorEnabled = false
  ).test("Slf4jLoggerInternal.mapK is still lazy") { testLogger =>
    val slf4jLogger = Slf4jLogger.getLoggerFromSlf4j[IO](testLogger).mapK[IO](FunctionK.id)
    val ctx = tag("lazy")
    // If these are lazy the way they need to be, the message won't be evaluated until
    // after the log level has been checked
    prepareMDC >>
      slf4jLogger.trace(die()).assert >>
      slf4jLogger.debug(die()).assert >>
      slf4jLogger.info(die()).assert >>
      slf4jLogger.warn(die()).assert >>
      slf4jLogger.error(die()).assert >>
      slf4jLogger.trace(throwable)(die()).assert >>
      slf4jLogger.debug(throwable)(die()).assert >>
      slf4jLogger.info(throwable)(die()).assert >>
      slf4jLogger.warn(throwable)(die()).assert >>
      slf4jLogger.error(throwable)(die()).assert >>
      slf4jLogger.trace(ctx)(die()).assert >>
      slf4jLogger.debug(ctx)(die()).assert >>
      slf4jLogger.info(ctx)(die()).assert >>
      slf4jLogger.warn(ctx)(die()).assert >>
      slf4jLogger.error(ctx)(die()).assert >>
      slf4jLogger.trace(ctx, throwable)(die()).assert >>
      slf4jLogger.debug(ctx, throwable)(die()).assert >>
      slf4jLogger.info(ctx, throwable)(die()).assert >>
      slf4jLogger.warn(ctx, throwable)(die()).assert >>
      slf4jLogger.error(ctx, throwable)(die()).assert >>
      validateMDC
  }

  testLoggerFixture(
    traceEnabled = false,
    debugEnabled = false,
    infoEnabled = false,
    warnEnabled = false,
    errorEnabled = false
  ).test("Slf4jLoggerInternal.withModifiedString is still lazy") { testLogger =>
    val slf4jLogger =
      Slf4jLogger.getLoggerFromSlf4j[IO](testLogger).withModifiedString(_.toUpperCase)
    val ctx = tag("lazy")
    // If these are lazy the way they need to be, the message won't be evaluated until
    // after the log level has been checked
    prepareMDC >>
      slf4jLogger.trace(die()).assert >>
      slf4jLogger.debug(die()).assert >>
      slf4jLogger.info(die()).assert >>
      slf4jLogger.warn(die()).assert >>
      slf4jLogger.error(die()).assert >>
      slf4jLogger.trace(throwable)(die()).assert >>
      slf4jLogger.debug(throwable)(die()).assert >>
      slf4jLogger.info(throwable)(die()).assert >>
      slf4jLogger.warn(throwable)(die()).assert >>
      slf4jLogger.error(throwable)(die()).assert >>
      slf4jLogger.trace(ctx)(die()).assert >>
      slf4jLogger.debug(ctx)(die()).assert >>
      slf4jLogger.info(ctx)(die()).assert >>
      slf4jLogger.warn(ctx)(die()).assert >>
      slf4jLogger.error(ctx)(die()).assert >>
      slf4jLogger.trace(ctx, throwable)(die()).assert >>
      slf4jLogger.debug(ctx, throwable)(die()).assert >>
      slf4jLogger.info(ctx, throwable)(die()).assert >>
      slf4jLogger.warn(ctx, throwable)(die()).assert >>
      slf4jLogger.error(ctx, throwable)(die()).assert >>
      validateMDC
  }

  testLoggerFixture(
    traceEnabled = false,
    debugEnabled = false,
    infoEnabled = false,
    warnEnabled = false,
    errorEnabled = false
  ).test("Slf4jLoggerInternal.addContext is still lazy") { testLogger =>
    val slf4jLogger = Slf4jLogger.getLoggerFromSlf4j[IO](testLogger).addContext(Map("bar" -> "foo"))
    val ctx = tag("lazy")
    // If these are lazy the way they need to be, the message won't be evaluated until
    // after the log level has been checked
    prepareMDC >>
      slf4jLogger.trace(die()).assert >>
      slf4jLogger.debug(die()).assert >>
      slf4jLogger.info(die()).assert >>
      slf4jLogger.warn(die()).assert >>
      slf4jLogger.error(die()).assert >>
      slf4jLogger.trace(throwable)(die()).assert >>
      slf4jLogger.debug(throwable)(die()).assert >>
      slf4jLogger.info(throwable)(die()).assert >>
      slf4jLogger.warn(throwable)(die()).assert >>
      slf4jLogger.error(throwable)(die()).assert >>
      slf4jLogger.trace(ctx)(die()).assert >>
      slf4jLogger.debug(ctx)(die()).assert >>
      slf4jLogger.info(ctx)(die()).assert >>
      slf4jLogger.warn(ctx)(die()).assert >>
      slf4jLogger.error(ctx)(die()).assert >>
      slf4jLogger.trace(ctx, throwable)(die()).assert >>
      slf4jLogger.debug(ctx, throwable)(die()).assert >>
      slf4jLogger.info(ctx, throwable)(die()).assert >>
      slf4jLogger.warn(ctx, throwable)(die()).assert >>
      slf4jLogger.error(ctx, throwable)(die()).assert >>
      validateMDC
  }

  testLoggerFixture().test("Slf4jLoggerInternal gets the dispatching right (msg)") { testLogger =>
    val slf4jLogger = Slf4jLogger.getLoggerFromSlf4j[IO](testLogger)
    prepareMDC >>
      slf4jLogger.trace("trace").assert >>
      slf4jLogger.debug("debug").assert >>
      slf4jLogger.info("info").assert >>
      slf4jLogger.warn("warn").assert >>
      slf4jLogger.error("error").assert >>
      IO(testLogger.logs())
        .map(toDeferredLogs)
        .assertEquals(
          List(
            DeferredLogMessage.trace(Map.empty, none, () => "trace"),
            DeferredLogMessage.debug(Map.empty, none, () => "debug"),
            DeferredLogMessage.info(Map.empty, none, () => "info"),
            DeferredLogMessage.warn(Map.empty, none, () => "warn"),
            DeferredLogMessage.error(Map.empty, none, () => "error")
          )
        ) >>
      validateMDC
  }

  testLoggerFixture().test("Slf4jLoggerInternal gets the dispatching right (msg + error)") {
    testLogger =>
      val slf4jLogger = Slf4jLogger.getLoggerFromSlf4j[IO](testLogger)
      prepareMDC >>
        slf4jLogger.trace(throwable)("trace").assert >>
        slf4jLogger.debug(throwable)("debug").assert >>
        slf4jLogger.info(throwable)("info").assert >>
        slf4jLogger.warn(throwable)("warn").assert >>
        slf4jLogger.error(throwable)("error").assert >>
        IO(testLogger.logs())
          .map(toDeferredLogs)
          .assertEquals(
            List(
              DeferredLogMessage.trace(Map.empty, throwable.some, () => "trace"),
              DeferredLogMessage.debug(Map.empty, throwable.some, () => "debug"),
              DeferredLogMessage.info(Map.empty, throwable.some, () => "info"),
              DeferredLogMessage.warn(Map.empty, throwable.some, () => "warn"),
              DeferredLogMessage.error(Map.empty, throwable.some, () => "error")
            )
          ) >>
        validateMDC
  }

  testLoggerFixture().test("Slf4jLoggerInternal gets the dispatching right (msg + context)") {
    testLogger =>
      val slf4jLogger = Slf4jLogger.getLoggerFromSlf4j[IO](testLogger)
      prepareMDC >>
        slf4jLogger.trace(tag("trace"))("trace").assert >>
        slf4jLogger.debug(tag("debug"))("debug").assert >>
        slf4jLogger.info(tag("info"))("info").assert >>
        slf4jLogger.warn(tag("warn"))("warn").assert >>
        slf4jLogger.error(tag("error"))("error").assert >>
        IO(testLogger.logs())
          .map(toDeferredLogs)
          .assertEquals(
            List(
              DeferredLogMessage.trace(tag("trace"), none, () => "trace"),
              DeferredLogMessage.debug(tag("debug"), none, () => "debug"),
              DeferredLogMessage.info(tag("info"), none, () => "info"),
              DeferredLogMessage.warn(tag("warn"), none, () => "warn"),
              DeferredLogMessage.error(tag("error"), none, () => "error")
            )
          ) >>
        validateMDC
  }

  testLoggerFixture().test(
    "Slf4jLoggerInternal gets the dispatching right (msg + context + error"
  ) { testLogger =>
    val slf4jLogger = Slf4jLogger.getLoggerFromSlf4j[IO](testLogger)
    prepareMDC >>
      slf4jLogger.trace(tag("trace"), throwable)("trace").assert >>
      slf4jLogger.debug(tag("debug"), throwable)("debug").assert >>
      slf4jLogger.info(tag("info"), throwable)("info").assert >>
      slf4jLogger.warn(tag("warn"), throwable)("warn").assert >>
      slf4jLogger.error(tag("error"), throwable)("error").assert >>
      IO(testLogger.logs())
        .map(toDeferredLogs)
        .assertEquals(
          List(
            DeferredLogMessage.trace(tag("trace"), throwable.some, () => "trace"),
            DeferredLogMessage.debug(tag("debug"), throwable.some, () => "debug"),
            DeferredLogMessage.info(tag("info"), throwable.some, () => "info"),
            DeferredLogMessage.warn(tag("warn"), throwable.some, () => "warn"),
            DeferredLogMessage.error(tag("error"), throwable.some, () => "error")
          )
        ) >>
      validateMDC
  }
}
