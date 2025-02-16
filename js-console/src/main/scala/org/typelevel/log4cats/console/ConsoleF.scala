/*
 * Copyright 2018 Typelevel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.typelevel.log4cats
package console

import cats.effect.kernel.Sync

private[console] class ConsoleF[F[_]: Sync] {
  def info(message: Any, optionalParams: Any*): F[Unit] =
    Sync[F].delay(Console.info(message, optionalParams*))
  def warn(message: Any, optionalParams: Any*): F[Unit] =
    Sync[F].delay(Console.warn(message, optionalParams*))
  def error(message: Any, optionalParams: Any*): F[Unit] =
    Sync[F].delay(Console.error(message, optionalParams*))
  def debug(message: Any, optionalParams: Any*): F[Unit] =
    Sync[F].delay(Console.debug(message, optionalParams*))
}

private[console] object ConsoleF {
  def apply[F[_]: ConsoleF]: ConsoleF[F] = implicitly

  implicit def syncInstance[F[_]: Sync]: ConsoleF[F] = new ConsoleF[F]
}
