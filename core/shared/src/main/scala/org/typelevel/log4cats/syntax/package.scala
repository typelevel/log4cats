/*
 * Copyright 2018 Christopher Davenport
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

package object syntax {
  implicit final class LoggerInterpolator(private val sc: StringContext) extends AnyVal {
    def error[F[_]](message: Any*)(implicit logger: Logger[F]): F[Unit] =
      logger.error(sc.s(message: _*))

    def warn[F[_]](message: Any*)(implicit logger: Logger[F]): F[Unit] =
      logger.warn(sc.s(message: _*))

    def info[F[_]](message: Any*)(implicit logger: Logger[F]): F[Unit] =
      logger.info(sc.s(message: _*))

    def debug[F[_]](message: Any*)(implicit logger: Logger[F]): F[Unit] =
      logger.debug(sc.s(message: _*))

    def trace[F[_]](message: Any*)(implicit logger: Logger[F]): F[Unit] =
      logger.trace(sc.s(message: _*))
  }
}
