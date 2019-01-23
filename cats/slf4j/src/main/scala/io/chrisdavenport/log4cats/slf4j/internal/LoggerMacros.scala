/**
 * Copyright 2013-2017 Sarah Gerweck
 * see: https://github.com/Log4s/log4s
 *
 * Modifications copyright (C) 2018 Christopher Davenport
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
 * 
 * Modifications:
 * - Major Rework
 */
package io.chrisdavenport.log4cats.slf4j.internal

import scala.annotation.tailrec
import scala.reflect.macros.blackbox

/** Macros that support the logging system.
 *
 *  See for handling call-by-name-parameters in macros
 *  https://issues.scala-lang.org/browse/SI-5778
 *
 * @author Sarah Gerweck <sarah@atscale.com>
 */
private[slf4j] class GetLoggerMacros(val c: blackbox.Context) {

  final def safeCreateImpl[F: c.WeakTypeTag](f: c.Expr[F]) = getLoggerImpl[F](f, true)

  final def unsafeCreateImpl[F: c.WeakTypeTag](f: c.Expr[F]) = getLoggerImpl[F](f, false)

  /** Get a logger by reflecting the enclosing class name. */
  private def getLoggerImpl[F: c.WeakTypeTag](f: c.Expr[F], delayed: Boolean) = {
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

    def loggerByParam(param: c.Tree) = {
      val unsafeCreate = q"_root_.io.chrisdavenport.log4cats.slf4j.Slf4jLogger.getLoggerFromSlf4j(_root_.org.slf4j.LoggerFactory.getLogger(...${List(
        param)}))($f)"
      if (delayed)
        q"_root_.cats.effect.Sync.apply($f).delay(...$unsafeCreate)"
      else
        unsafeCreate
    }

    def loggerBySymbolName(s: Symbol) = {
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
      loggerByParam(q"${fullName(s)}")
    }

    def loggerByType(s: Symbol) = {
      val typeSymbol: ClassSymbol = (if (s.isModule) s.asModule.moduleClass else s).asClass
      val typeParams = typeSymbol.typeParams

      if (typeParams.isEmpty) {
        loggerByParam(q"classOf[$typeSymbol]")
      } else {
        if (typeParams.exists(_.asType.typeParams.nonEmpty)) {
          /* We have at least one higher-kinded type: fall back to by-name logger construction, as
           * there's no simple way to declare a higher-kinded type with an "any" parameter. */
          loggerBySymbolName(s)
        } else {
          val typeArgs = List.fill(typeParams.length)(WildcardType)
          val typeConstructor = tq"$typeSymbol[..${typeArgs}]"
          loggerByParam(q"classOf[$typeConstructor]")
        }
      }
    }

    @inline def isInnerClass(s: Symbol) =
      s.isClass && !(s.owner.isPackage)

    val instanceByName = Slf4jLoggerInternal.singletonsByName && (cls.isModule || cls.isModuleClass) || cls.isClass && isInnerClass(
      cls
    )

    if (instanceByName) {
      loggerBySymbolName(cls)
    } else {
      loggerByType(cls)
    }
  }
}