package io.chrisdavenport.log4cats.slf4j

import cats.effect.Sync
import io.chrisdavenport.log4cats.SelfAwareStructuredLogger

class Slf4jLoggerSimpleClassMacroTest {
  def loggerF[F[_]: Sync]: F[SelfAwareStructuredLogger[F]] = Slf4jLogger.create[F]
  def logger[F[_]: Sync]: SelfAwareStructuredLogger[F] = Slf4jLogger.unsafeCreate[F]
}

class Slf4jLoggerParameterizedClassMacroTest[A] {
  def loggerF[F[_]: Sync]: F[SelfAwareStructuredLogger[F]] = Slf4jLogger.create[F]
  def logger[F[_]: Sync]: SelfAwareStructuredLogger[F] = Slf4jLogger.unsafeCreate[F]
}

class Slf4jLoggerHKTMacroTest[F[_]: Sync] {
  def loggerF: F[SelfAwareStructuredLogger[F]] = Slf4jLogger.create[F]
  def logger: SelfAwareStructuredLogger[F] = Slf4jLogger.unsafeCreate[F]
}

object Slf4jLoggerModuleMacroTest {
  def loggerF[F[_]: Sync]: F[SelfAwareStructuredLogger[F]] = Slf4jLogger.create[F]
  def logger[F[_]: Sync]: SelfAwareStructuredLogger[F] = Slf4jLogger.unsafeCreate[F]
}

class Slf4jLoggerOuterClassMacroTest {
  class Slf4jLoggerInnerClassMacroTest {
    def loggerF[F[_]: Sync]: F[SelfAwareStructuredLogger[F]] = Slf4jLogger.create[F]
    def logger[F[_]: Sync]: SelfAwareStructuredLogger[F] = Slf4jLogger.unsafeCreate[F]
  }
}
