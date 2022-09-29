package org.typelevel.log4cats
package console

import cats.effect.Sync

class ConsoleF[F[_] : Sync] {
  def info(message: Any, optionalParams: Any*): F[Unit] = Sync[F].delay(Console.info(message, optionalParams:_*))
  def warn(message: Any, optionalParams: Any*): F[Unit] = Sync[F].delay(Console.warn(message, optionalParams:_*))
  def error(message: Any, optionalParams: Any*): F[Unit] = Sync[F].delay(Console.error(message, optionalParams:_*))
  def debug(message: Any, optionalParams: Any*): F[Unit] = Sync[F].delay(Console.debug(message, optionalParams:_*))
}

object ConsoleF {
  def apply[F[_] : ConsoleF]: ConsoleF[F] = implicitly

  implicit def syncInstance[F[_] : Sync]: ConsoleF[F] = new ConsoleF[F]
}
