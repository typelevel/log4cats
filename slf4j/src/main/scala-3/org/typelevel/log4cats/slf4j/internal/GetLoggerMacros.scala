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

package org.typelevel.log4cats.slf4j.internal

import cats.effect.Sync
import org.slf4j.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.internal.LoggerNameMacro

import scala.annotation.tailrec
import scala.quoted.*

private[slf4j] object GetLoggerMacros {

  def getLoggerImpl[F[_]: Type](
      F: Expr[Sync[F]]
  )(using qctx: Quotes): Expr[SelfAwareStructuredLogger[F]] = {
    val name = LoggerNameMacro.getLoggerNameImpl
    '{ Slf4jLogger.getLoggerFromSlf4j(LoggerFactory.getLogger($name))($F) }
  }

  def createImpl[F[_]: Type](
      F: Expr[Sync[F]]
  )(using qctx: Quotes): Expr[F[SelfAwareStructuredLogger[F]]] = {
    val logger = getLoggerImpl(F)
    '{ $F.delay($logger) }
  }
}
