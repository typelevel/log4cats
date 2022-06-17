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

package org.typelevel.log4cats.slf4j

import cats.effect.Sync
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.internal._
import org.slf4j.Logger as JLogger
import scala.annotation.nowarn

trait Slf4jLoggerCompat {

  // for binary compability
  private[slf4j] inline def createMacro[F[_]](F: Sync[F]): F[SelfAwareStructuredLogger[F]] =
    ${ GetLoggerMacros.createImpl('F) }

  private[slf4j] inline def getLoggerMacro[F[_]](using F: Sync[F]): SelfAwareStructuredLogger[F] =
    ${ GetLoggerMacros.getLoggerImpl('F) }

}
