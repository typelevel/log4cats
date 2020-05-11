package io.chrisdavenport.log4cats.slf4j
package internal

import org.specs2.mutable.Specification
import cats.effect.IO

import org.slf4j.MDC
import scala.concurrent.ExecutionContext
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import scala.concurrent.ExecutionContextExecutorService
import cats.implicits._
import cats.effect.Fiber

class Slf4jLoggerInternalSpec extends Specification {

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

  "Slf4jLoggerInternal" should {
    "reset after logging" in {
      val variable = "foo"
      val initial = "yellow"
      MDC.put(variable, initial)

      Slf4jLogger
        .getLogger[IO]
        .info(Map(variable -> "bar"))("A log went here")
        .unsafeRunSync

      val out = MDC.get(variable)
      out must_=== initial
    }

    "reset on cancel" in {
      val variable = "foo"
      val initial = "initial"

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
        IO { runBlockingOn { MDC.get(variable) }(ec) }

      val getVariables = (
        getVariableOn(loggerThread),
        getVariableOn(finalizerThread),
        IO(MDC.get(variable))
      ).tupled

      val result =
        IO {
          runBlockingOn { MDC.put(variable, initial) }(loggerThread)
          runBlockingOn { MDC.put(variable, initial) }(finalizerThread)
        } *>
          performLogging
            .start(IO.contextShift(loggerThread))
            .flatMap(
              performCancel(_)
                .start(IO.contextShift(finalizerThread))
                .flatMap(_.join)
            ) *>
          IO(finishedLog.await()) *>
          getVariables

      val (out1, out2, outMain) = result.unsafeRunSync()

      try {
        out1 must_=== initial
        out2 must_=== initial
        outMain must_=== null
      } finally killThreads(List(loggerThread, finalizerThread))
    }
  }
}
