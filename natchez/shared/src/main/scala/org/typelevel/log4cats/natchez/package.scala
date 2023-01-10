package org.typelevel.log4cats

import _root_.natchez._
import cats._
import cats.syntax.all._
import org.typelevel.log4cats.extras.LogLevel

package object natchez {
  /**
   * `LogLevel` as an OpenTelemetry `SeverityNumber`
   *
   * See https://opentelemetry.io/docs/reference/specification/logs/data-model/#field-severitynumber
   *
   * @return LogLevel as an OpenTelemetry SeverityNumber, converted to the TraceValue ADT
   */
  private val logLevelToSeverityNumber: LogLevel => TraceValue = {
    case LogLevel.Trace => 1
    case LogLevel.Debug => 5
    case LogLevel.Info => 9
    case LogLevel.Warn => 13
    case LogLevel.Error => 17
  }

  /**
   * `LogLevel` as an OpenTelemetry `SeverityText`
   *
   * See https://opentelemetry.io/docs/reference/specification/logs/data-model/#field-severitytext
   * and https://opentelemetry.io/docs/reference/specification/logs/data-model/#displaying-severity
   *
   * @return LogLevel as an OpenTelemetry SeverityNumber, converted to the TraceValue ADT
   */
  private val logLevelToSeverityText: LogLevel => TraceValue = {
    case LogLevel.Trace => "TRACE"
    case LogLevel.Debug => "DEBUG"
    case LogLevel.Info => "INFO"
    case LogLevel.Warn => "WARN"
    case LogLevel.Error => "ERROR"
  }

  private val mapContextToTraceValue: Map[String, String] => List[(String, TraceValue)] =
    _.toList.nested.map(TraceValue.StringValue).value

  implicit def TraceLogger[F[_] : Trace : Applicative]: StructuredLogger[F] = new StructuredLogger[F] {
    private def log(logLevel: LogLevel,
                    ctx: Map[String, String],
                    maybeThrowable: Option[Throwable],
                    msg: => String): F[Unit] = {
      val attributes =
        "event" -> TraceValue.StringValue(msg) ::
          "severity_number" -> logLevelToSeverityNumber(logLevel) ::
          "severity_text" -> logLevelToSeverityText(logLevel) ::
          mapContextToTraceValue(ctx)

      Trace[F].log(attributes: _*) *> maybeThrowable.fold(().pure[F])(Trace[F].attachError)
    }

    override def trace(ctx: Map[String, String])(msg: => String): F[Unit] = log(LogLevel.Trace, ctx, None, msg)
    override def trace(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] = log(LogLevel.Trace, ctx, t.some, msg)
    override def trace(message: => String): F[Unit] = log(LogLevel.Trace, Map.empty, None, message)
    override def trace(t: Throwable)(message: => String): F[Unit] = log(LogLevel.Trace, Map.empty, t.some, message)

    override def debug(ctx: Map[String, String])(msg: => String): F[Unit] = log(LogLevel.Debug, ctx, None, msg)
    override def debug(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] = log(LogLevel.Debug, ctx, t.some, msg)
    override def debug(message: => String): F[Unit] = log(LogLevel.Debug, Map.empty, None, message)
    override def debug(t: Throwable)(message: => String): F[Unit] = log(LogLevel.Debug, Map.empty, t.some, message)

    override def info(ctx: Map[String, String])(msg: => String): F[Unit] = log(LogLevel.Info, ctx, None, msg)
    override def info(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] = log(LogLevel.Info, ctx, t.some, msg)
    override def info(message: => String): F[Unit] = log(LogLevel.Info, Map.empty, None, message)
    override def info(t: Throwable)(message: => String): F[Unit] = log(LogLevel.Info, Map.empty, t.some, message)

    override def warn(ctx: Map[String, String])(msg: => String): F[Unit] = log(LogLevel.Warn, ctx, None, msg)
    override def warn(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] = log(LogLevel.Warn, ctx, t.some, msg)
    override def warn(message: => String): F[Unit] = log(LogLevel.Warn, Map.empty, None, message)
    override def warn(t: Throwable)(message: => String): F[Unit] = log(LogLevel.Warn, Map.empty, t.some, message)

    override def error(ctx: Map[String, String])(msg: => String): F[Unit] = log(LogLevel.Error, ctx, None, msg)
    override def error(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] = log(LogLevel.Error, ctx, t.some, msg)
    override def error(message: => String): F[Unit] = log(LogLevel.Error, Map.empty, None, message)
    override def error(t: Throwable)(message: => String): F[Unit] = log(LogLevel.Error, Map.empty, t.some, message)
  }
}
