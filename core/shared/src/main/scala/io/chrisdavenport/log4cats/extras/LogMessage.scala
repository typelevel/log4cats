package io.chrisdavenport.log4cats.extras

import cats._
import cats.syntax.all._

final case class LogMessage(level: LogLevel, t: Option[Throwable], message: String)

object LogMessage {
  implicit val logMessageShow: Show[LogMessage] =
    Show.show[LogMessage](l => show"LogMessage(${l.level},${l.t.map(_.getMessage)},${l.message})")

}
