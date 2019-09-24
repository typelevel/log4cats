package io.chrisdavenport.log4cats.mtl

import cats.Applicative
import cats.mtl.FunctorTell
import cats.syntax.applicative._
import cats.syntax.option._
import io.chrisdavenport.log4cats.SelfAwareLogger
import io.chrisdavenport.log4cats.extras.{LogLevel, LogMessage}

object FunctorTellLogger {

  def apply[F[_]: Applicative: FunctorTell[?[_], G[LogMessage]], G[_]: Applicative](
      traceEnabled: Boolean = true,
      debugEnabled: Boolean = true,
      infoEnabled: Boolean = true,
      warnEnabled: Boolean = true,
      errorEnabled: Boolean = true
  ): SelfAwareLogger[F] =
    new FunctorTellLogger[F, G](traceEnabled, debugEnabled, infoEnabled, warnEnabled, errorEnabled)

}

final class FunctorTellLogger[F[_]: Applicative, G[_]: Applicative](
    traceEnabled: Boolean = true,
    debugEnabled: Boolean = true,
    infoEnabled: Boolean = true,
    warnEnabled: Boolean = true,
    errorEnabled: Boolean = true
)(implicit FT: FunctorTell[F, G[LogMessage]])
    extends SelfAwareLogger[F] {

  override def isTraceEnabled: F[Boolean] = traceEnabled.pure[F]
  override def isDebugEnabled: F[Boolean] = debugEnabled.pure[F]
  override def isInfoEnabled: F[Boolean] = infoEnabled.pure[F]
  override def isWarnEnabled: F[Boolean] = warnEnabled.pure[F]
  override def isErrorEnabled: F[Boolean] = errorEnabled.pure[F]

  override def trace(t: Throwable)(message: => String): F[Unit] =
    build(traceEnabled, LogLevel.Trace, t.some, message)
  override def trace(message: => String): F[Unit] =
    build(traceEnabled, LogLevel.Trace, None, message)

  override def debug(t: Throwable)(message: => String): F[Unit] =
    build(debugEnabled, LogLevel.Debug, t.some, message)
  override def debug(message: => String): F[Unit] =
    build(debugEnabled, LogLevel.Debug, None, message)

  override def info(t: Throwable)(message: => String): F[Unit] =
    build(infoEnabled, LogLevel.Info, t.some, message)
  override def info(message: => String): F[Unit] =
    build(infoEnabled, LogLevel.Info, None, message)

  override def warn(t: Throwable)(message: => String): F[Unit] =
    build(warnEnabled, LogLevel.Warn, t.some, message)
  override def warn(message: => String): F[Unit] =
    build(warnEnabled, LogLevel.Warn, None, message)

  override def error(t: Throwable)(message: => String): F[Unit] =
    build(errorEnabled, LogLevel.Error, t.some, message)
  override def error(message: => String): F[Unit] =
    build(errorEnabled, LogLevel.Error, None, message)

  @inline private def build(
      enabled: Boolean,
      level: LogLevel,
      t: Option[Throwable],
      message: => String
  ): F[Unit] =
    FT.tell(LogMessage(level, t, message).pure[G]).whenA(enabled)

}
