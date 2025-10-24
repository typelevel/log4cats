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

import cats.FlatMap
import cats.mtl.Local

object LocalHelpers {
  implicit class LoggerOps[F[_]](sl: SelfAwareStructuredLogger[F]) {
    def withLocalContext(implicit
        local: Local[F, Map[String, String]],
        F: FlatMap[F]
    ): SelfAwareStructuredLogger[F] =
      SelfAwareStructuredLogger.withContextF(sl)(local.ask)
  }

  implicit class FactoryOps[F[_]](lf: LoggerFactory[F]) {
    def withLocalContext(implicit
        local: Local[F, Map[String, String]],
        F: FlatMap[F]
    ): LoggerFactory[F] =
      LoggerFactory.withContextF(lf)(local.ask)
  }
}
