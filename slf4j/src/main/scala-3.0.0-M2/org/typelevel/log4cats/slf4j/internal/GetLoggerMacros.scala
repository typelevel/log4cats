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

package org.typelevel.log4cats.slf4j.internal

import cats.effect.Sync
import org.slf4j.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.SelfAwareStructuredLogger
import scala.annotation.tailrec
import scala.quoted._

private[slf4j] object GetLoggerMacros {

  def getLoggerImpl[F[_]: Type](F: Expr[Sync[F]])(using qctx: Quotes): Expr[SelfAwareStructuredLogger[F]] = {
    import qctx.reflect._

    @tailrec def findEnclosingClass(sym: Symbol): Symbol = {
      sym match {
        case s if s.isNoSymbol =>
          report.throwError("Couldn't find an enclosing class or module for the logger")
        case s if s.isClassDef =>
          s
        case other =>
          /* We're not in a module or a class, so we're probably inside a member definition. Recurse upward. */
          findEnclosingClass(other.owner)
      }
    }

    def logger(s: Symbol): Expr[SelfAwareStructuredLogger[F]] = {
      def fullName(s: Symbol): String = {
        val flags = s.flags
        if (flags.is(Flags.Package)) {
          s.fullName
        }
        else if (s.isClassDef) {
          if (flags.is(Flags.ModuleClass & Flags.Object)) {
            if (s.name == "package$") {
              fullName(s.owner)
            }
            else {
              val chomped = s.name.stripSuffix("$")
              fullName(s.owner) + "." + chomped
            }
          }
          else {
            fullName(s.owner) + "." + s.name
          }
        }
        else {
          fullName(s.owner)
        }
      }

      val name = Expr(fullName(s))
      '{ Slf4jLogger.getLoggerFromSlf4j(LoggerFactory.getLogger($name))($F) }
    }

    val cls = findEnclosingClass(Symbol.spliceOwner)
    logger(cls)
  }

  def createImpl[F[_]: Type](F: Expr[Sync[F]])(using qctx: Quotes): Expr[F[SelfAwareStructuredLogger[F]]] = {
    val logger = getLoggerImpl(F)
    '{$F.delay($logger)}
  }
}
