package io.chrisdavenport.log4cats.slf4j

import cats.effect._
import io.chrisdavenport.log4cats.SelfAwareStructuredLogger

class Slf4jLoggerSimpleClassMacroTest {
  def loggerF[F[_]: Sync]: F[SelfAwareStructuredLogger[F]] = Slf4jLogger.create[F]
  def logger[F[_]: Sync]: SelfAwareStructuredLogger[F] = Slf4jLogger.getLogger[F]
}

class Slf4jLoggerParameterizedClassMacroTest[A] {
  def loggerF[F[_]: Sync]: F[SelfAwareStructuredLogger[F]] = Slf4jLogger.create[F]
  def logger[F[_]: Sync]: SelfAwareStructuredLogger[F] = Slf4jLogger.getLogger[F]
}

class Slf4jLoggerHKTMacroTest[F[_]: Sync] {
  def loggerF: F[SelfAwareStructuredLogger[F]] = Slf4jLogger.create[F]
  def logger: SelfAwareStructuredLogger[F] = Slf4jLogger.getLogger[F]
}

object Slf4jLoggerModuleMacroTest {
  def loggerF[F[_]: Sync]: F[SelfAwareStructuredLogger[F]] = Slf4jLogger.create[F]
  def logger[F[_]: Sync]: SelfAwareStructuredLogger[F] = Slf4jLogger.getLogger[F]
}

class Slf4jLoggerOuterClassMacroTest {
  class Slf4jLoggerInnerClassMacroTest {
    def loggerF[F[_]: Sync]: F[SelfAwareStructuredLogger[F]] = Slf4jLogger.create[F]
    def logger[F[_]: Sync]: SelfAwareStructuredLogger[F] = Slf4jLogger.getLogger[F]
  }
}

object LoggingBaseline {
  val t = new Throwable
  def logger[F[_]: Sync]: SelfAwareStructuredLogger[F] = Slf4jLogger.getLogger[F]

  val traceM = logger[IO].trace("")
  val traceTM = logger[IO].trace(t)("")
  val traceCM = logger[IO].trace(Map.empty[String, String])("")

  val debugM = logger[IO].debug("")
  val debugTM = logger[IO].debug(t)("")
  val debugCM = logger[IO].debug(Map.empty[String, String])("")

  val infoM = logger[IO].info("")
  val infoTM = logger[IO].info(t)("")
  val infoCM = logger[IO].info(Map.empty[String, String])("")

  val warnM = logger[IO].warn("")
  val warnTM = logger[IO].warn(t)("")
  val warnCM = logger[IO].warn(Map.empty[String, String])("")

  val errorM = logger[IO].error("")
  val errorTM = logger[IO].error(t)("")
  val errorCM = logger[IO].error(Map.empty[String, String])("")

}
