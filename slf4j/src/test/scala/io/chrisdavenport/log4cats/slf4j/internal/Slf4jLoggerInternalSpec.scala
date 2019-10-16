package io.chrisdavenport.log4cats.slf4j
package internal

import org.specs2.mutable.Specification
import cats.effect.IO

import org.slf4j.MDC

class Slf4jLoggerInternalSpec extends Specification {

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
  }
}
