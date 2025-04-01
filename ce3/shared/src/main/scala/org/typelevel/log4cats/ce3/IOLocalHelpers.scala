package org.typelevel.log4cats.ce3

import cats.effect.IO
import cats.mtl.Local
import org.typelevel.log4cats.{LoggerFactory, SelfAwareStructuredLogger}

object IOLocalHelpers {
  def loggerWithContext(
      sl: SelfAwareStructuredLogger[IO]
      )(implicit local: Local[IO, Map[String, String]]): SelfAwareStructuredLogger[IO] =
    SelfAwareStructuredLogger.withContextF(sl)(local.ask)

  def factoryWithContext(
      lf: LoggerFactory[IO]
      )(implicit local: Local[IO, Map[String, String]]): LoggerFactory[IO] =
    LoggerFactory.withContextF(lf)(local.ask)
}