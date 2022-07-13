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
package internal

import scala.annotation.tailrec
import scala.quoted.*

private[log4cats] object LoggerNameMacro {
  def getLoggerName(using qctx: Quotes): Expr[LoggerName] = {
    val name = getLoggerNameImpl
    '{ new LoggerName($name) }
  }

  def getLoggerNameImpl(using qctx: Quotes): Expr[String] = {
    import qctx.reflect._

    @tailrec def findEnclosingClass(sym: Symbol): Symbol = {
      sym match {
        case s if s.isNoSymbol =>
          report.errorAndAbort("Couldn't find an enclosing class or module for the logger")
        case s if s.isClassDef =>
          s
        case other =>
          /* We're not in a module or a class, so we're probably inside a member definition. Recurse upward. */
          findEnclosingClass(other.owner)
      }
    }

    def symbolName(s: Symbol): Expr[String] = {
      def fullName(s: Symbol): String = {
        val flags = s.flags
        if (flags.is(Flags.Package)) {
          s.fullName
        } else if (s.isClassDef) {
          if (flags.is(Flags.Module)) {
            if (s.name == "package$") {
              fullName(s.owner)
            } else {
              val chomped = s.name.stripSuffix("$")
              fullName(s.owner) + "." + chomped
            }
          } else {
            fullName(s.owner) + "." + s.name
          }
        } else {
          fullName(s.owner)
        }
      }

      Expr(fullName(s).stripSuffix("$"))
    }

    val cls = findEnclosingClass(Symbol.spliceOwner)
    symbolName(cls)
  }
}
