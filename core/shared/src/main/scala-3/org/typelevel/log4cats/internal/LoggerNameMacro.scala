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
          report.throwError("Couldn't find an enclosing class or module for the logger")
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
