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

package org.typelevel.log4cats.internal

import scala.annotation.tailrec
import scala.reflect.macros.blackbox

private[log4cats] class LoggerNameMacro(val c: blackbox.Context) {
  final def getLoggerName = SharedLoggerNameMacro.getLoggerNameImpl(c)
}

private[log4cats] object SharedLoggerNameMacro {

  /** Get a logger by reflecting the enclosing class name. */
  private[log4cats] def getLoggerNameImpl(c: blackbox.Context) = {
    import c.universe._

    @tailrec def findEnclosingClass(sym: c.universe.Symbol): c.universe.Symbol = {
      sym match {
        case NoSymbol =>
          c.abort(c.enclosingPosition, s"Couldn't find an enclosing class or module for the logger")
        case s if s.isModule || s.isClass =>
          s
        case other =>
          /* We're not in a module or a class, so we're probably inside a member definition. Recurse upward. */
          findEnclosingClass(other.owner)
      }
    }

    val cls = findEnclosingClass(c.internal.enclosingOwner)

    assert(cls.isModule || cls.isClass, "Enclosing class is always either a module or a class")

    def nameBySymbol(s: Symbol) = {
      def fullName(s: Symbol): String = {
        @inline def isPackageObject = (
          (s.isModule || s.isModuleClass)
            && s.owner.isPackage
            && s.name.decodedName.toString == termNames.PACKAGE.decodedName.toString
        )

        if (s.isModule || s.isClass) {
          if (isPackageObject) {
            s.owner.fullName
          } else if (s.owner.isStatic) {
            s.fullName
          } else {
            fullName(s.owner) + "." + s.name.encodedName.toString
          }
        } else {
          fullName(s.owner)
        }
      }

      q"new _root_.org.typelevel.log4cats.LoggerName(${fullName(s)})"
    }

    def nameByType(s: Symbol) = {
      val typeSymbol: ClassSymbol = (if (s.isModule) s.asModule.moduleClass else s).asClass
      val typeParams = typeSymbol.typeParams

      if (typeParams.isEmpty) {
        q"new _root_.org.typelevel.log4cats.LoggerName(_root_.scala.Predef.classOf[$typeSymbol].getName)"
      } else {
        if (typeParams.exists(_.asType.typeParams.nonEmpty)) {
          /* We have at least one higher-kinded type: fall back to by-name logger construction, as
           * there's no simple way to declare a higher-kinded type with an "any" parameter. */
          nameBySymbol(s)
        } else {
          val typeArgs = List.fill(typeParams.length)(WildcardType)
          val typeConstructor = tq"$typeSymbol[..${typeArgs}]"
          q"new _root_.org.typelevel.log4cats.LoggerName(_root_.scala.Predef.classOf[$typeConstructor].getName)"
        }
      }
    }

    @inline def isInnerClass(s: Symbol) =
      s.isClass && !(s.owner.isPackage)

    val instanceByName =
      (true && cls.isModule || cls.isModuleClass) || (cls.isClass && isInnerClass(
        cls
      ))

    if (instanceByName) {
      nameBySymbol(cls)
    } else {
      nameByType(cls)
    }
  }
}
