package org.typelevel.log4cats

import cats.mtl.Local
import org.typelevel.log4cats.{LoggerFactory, SelfAwareStructuredLogger}

object LocalHelpers {
  implicit class LoggerOps[F[_]](sl: SelfAwareStructuredLogger[F]) {
    def withLocalContext(implicit
        local: Local[F, Map[String, String]]
    ): SelfAwareStructuredLogger[F] =
      SelfAwareStructuredLogger.withContextF(sl)(local.ask)
  }

  implicit class FactoryOps[F[_]](lf: LoggerFactory[F]) {
    def withLocalContext(implicit local: Local[F, Map[String, String]]): LoggerFactory[F] =
      LoggerFactory.withContextF(lf)(local.ask)
  }
}
