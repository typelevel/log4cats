package io.chrisdavenport.log4cats.extras

import cats._

sealed trait LogLevel
object LogLevel {
  case object Error extends LogLevel
  case object Warn extends LogLevel
  case object Info extends LogLevel
  case object Debug extends LogLevel
  case object Trace extends LogLevel

  implicit val logLevelShow: Show[LogLevel] = Show.show[LogLevel] {
    case Error => "LogLevel.Error"
    case Warn => "LogLevel.Warn"
    case Info => "LogLevel.Info"
    case Debug => "LogLevel.Debug"
    case Trace => "LogLevel.Trace"
  }

  implicit final val logLevelOrder: Order[LogLevel] =
    Order.by[LogLevel, Int] {
      case Error => 5
      case Warn => 4
      case Info => 3
      case Debug => 2
      case Trace => 1
    }
}
