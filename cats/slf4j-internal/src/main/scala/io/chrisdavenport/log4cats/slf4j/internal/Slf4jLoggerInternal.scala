package io.chrisdavenport.log4cats.slf4j.internal

import language.experimental.macros
import org.slf4j.{Logger => JLogger}
import cats.effect.Sync

private[slf4j] object Slf4jLoggerInternal {

  final val singletonsByName = true
  final val trailingDollar = false

  sealed trait LevelLogger[F[_]] extends Any {
    def isEnabled: F[Boolean]

    def apply(msg: => String): F[Unit]
    def apply(t: Throwable)(msg: => String): F[Unit]
  }
}

private[slf4j] final class Slf4jLoggerInternal[F[_]](val logger: JLogger)(implicit val F: Sync[F]){

  @inline def isTraceEnabled: F[Boolean] = F.delay(logger.isTraceEnabled)

  @inline def isDebugEnabled: F[Boolean] = F.delay(logger.isDebugEnabled)

  @inline def isInfoEnabled: F[Boolean] = F.delay(logger.isInfoEnabled)

  @inline def isWarnEnabled: F[Boolean] = F.delay(logger.isWarnEnabled)

  @inline def isErrorEnabled: F[Boolean] = F.delay(logger.isErrorEnabled)

  import ReflectiveLogMacros._

  // Internal Methods To Not Run into Macro Abstract implementation issues.
  def internalTraceTM(t: Throwable)(msg: => String): F[Unit] = macro traceTM[F]
  def internalTraceM(msg: => String): F[Unit] = macro traceM[F]
  def internalTraceMDC(ctx: (String, String)*)(msg: => String): F[Unit] = macro traceCM[F]

  def internalDebugTM(t: Throwable)(msg: => String): F[Unit] = macro debugTM[F]
  def internalDebugM(msg: => String): F[Unit] = macro debugM[F]
  def internalDebugMDC(ctx: (String, String)*)(msg: => String): F[Unit] = macro debugCM[F]

  def internalInfoTM(t: Throwable)(msg: => String): F[Unit] = macro infoTM[F]
  def internalInfoM(msg: => String): F[Unit] = macro infoM[F]
  def internalInfoMDC(ctx: (String, String)*)(msg: => String): F[Unit] = macro infoCM[F]

  def internalWarnTM(t: Throwable)(msg: => String): F[Unit] = macro warnTM[F]
  def internalWarnM(msg: => String): F[Unit] = macro warnM[F]
  def internalWarnMDC(ctx: (String, String)*)(msg: => String): F[Unit] = macro warnCM[F]

  def internalErrorTM(t: Throwable)(msg: => String): F[Unit] = macro errorTM[F]
  def internalErrorM(msg: => String): F[Unit] = macro errorM[F]
  def internalErrorMDC(ctx: (String, String)*)(msg: => String): F[Unit] = macro errorCM[F]

}
