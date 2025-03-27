/*package org.typelevel.log4cats.ce3

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
}*/
import cats.effect.{IO, IOLocal, Local}
import org.typelevel.log4cats.SelfAwareStructuredLogger

object IOLocalHelpers {
  def loggerWithContextFromIOLocal(ioLocal: IOLocal[Map[String, String]]): SelfAwareStructuredLogger[IO] = {
    new SelfAwareStructuredLogger[IO] {
      override def trace(ctx: Map[String, String])(msg: => String): IO[Unit] = 
        logWithContext(msg, _ => ctx)
      override def debug(ctx: Map[String, String])(msg: => String): IO[Unit] = 
        logWithContext(msg, _ => ctx)
      override def info(ctx: Map[String, String])(msg: => String): IO[Unit] = 
        logWithContext(msg, _ => ctx)
      override def warn(ctx: Map[String, String])(msg: => String): IO[Unit] = 
        logWithContext(msg, _ => ctx)
      override def error(ctx: Map[String, String])(msg: => String): IO[Unit] = 
        logWithContext(msg, _ => ctx)

      private def logWithContext(msg: String, contextMod: Map[String, String] => Map[String, String])
                               (implicit local: Local[IO, Map[String, String]]): IO[Unit] = {
        local.ask.flatMap { currentContext =>
          val newContext = contextMod(currentContext)
          local.local(IO(println(s"[$newContext] $msg")))(newContext)
        }
      }
    }
  }
}
