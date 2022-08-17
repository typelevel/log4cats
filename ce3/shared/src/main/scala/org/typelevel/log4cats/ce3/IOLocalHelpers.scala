package org.typelevel.log4cats.ce3

import cats.effect.{IO, IOLocal}
import org.typelevel.log4cats.{LoggerFactory, SelfAwareStructuredLogger}

object IOLocalHelpers {
  def loggerWithContextFromIOLocal(
      sl: SelfAwareStructuredLogger[IO],
      local: IOLocal[Map[String, String]]
  ): SelfAwareStructuredLogger[IO] = SelfAwareStructuredLogger.withContextF(sl)(local.get)

  def factoryWithContextFromIOLocal(
      lf: LoggerFactory[IO],
      local: IOLocal[Map[String, String]]
  ): LoggerFactory[IO] = LoggerFactory.withContextF(lf)(local.get)
}
