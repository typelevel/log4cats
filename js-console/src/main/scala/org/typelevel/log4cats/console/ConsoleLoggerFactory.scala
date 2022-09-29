package org.typelevel.log4cats
package console

import cats.effect._
import cats.syntax.all._

class ConsoleLoggerFactory[F[_] : Sync] extends LoggerFactory[F] {
  override def getLoggerFromName(name: String): SelfAwareStructuredLogger[F] = new ConsoleLogger[F]
  override def fromName(name: String): F[SelfAwareStructuredLogger[F]] = getLoggerFromName(name).pure[F].widen
}

object ConsoleLoggerFactory {
  def apply[F[_] : Sync]: ConsoleLoggerFactory[F] = new ConsoleLoggerFactory[F]
}
