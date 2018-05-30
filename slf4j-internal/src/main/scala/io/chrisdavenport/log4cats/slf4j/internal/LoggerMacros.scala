/**
 * Copyright 2013-2017 Sarah Gerweck
 * see: https://github.com/Log4s/log4s
 *
 * Modifications copyright (C) 2018 Lorand Szakacs
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
package io.chrisdavenport.log4cats.slf4j.internal

import scala.annotation.tailrec
import scala.reflect.macros.{blackbox, whitebox}

/** Macros that support the logging system.
 *
 *  See for handling call-by-name-parameters in macros
 *  https://issues.scala-lang.org/browse/SI-5778
 *
 * @author Sarah Gerweck <sarah@atscale.com>
 */
private[slf4j] object LoggerMacros {

  /** Get a logger by reflecting the enclosing class name. */
  final def getLoggerImpl[F: c.WeakTypeTag](c: blackbox.Context)(f: c.Expr[F]) = {
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

    def loggerByParam(param: c.Tree)(f: c.Expr[F]) =
      q"_root_.io.chrisdavenport.log4cats.slf4j.Slf4jLogger.unsafeFromSlf4j(_root_.org.slf4j.LoggerFactory.getLogger(...${List(
        param)}))($f)"

    def loggerBySymbolName(s: Symbol)(f: c.Expr[F]) = {
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
      loggerByParam(q"${fullName(s)}")(f)
    }

    def loggerByType(s: Symbol)(f: c.Expr[F]) = {
      val typeSymbol: ClassSymbol = (if (s.isModule) s.asModule.moduleClass else s).asClass
      val typeParams = typeSymbol.typeParams

      if (typeParams.isEmpty) {
        loggerByParam(q"classOf[$typeSymbol]")(f)
      } else {
        if (typeParams.exists(_.asType.typeParams.nonEmpty)) {
          /* We have at least one higher-kinded type: fall back to by-name logger construction, as
           * there's no simple way to declare a higher-kinded type with an "any" parameter. */
          loggerBySymbolName(s)(f)
        } else {
          val typeArgs = List.fill(typeParams.length)(WildcardType)
          val typeConstructor = tq"$typeSymbol[..${typeArgs}]"
          loggerByParam(q"classOf[$typeConstructor]")(f)
        }
      }
    }

    @inline def isInnerClass(s: Symbol) =
      s.isClass && !(s.owner.isPackage)

    val instanceByName = Slf4jLoggerInternal.singletonsByName && (cls.isModule || cls.isModuleClass) || cls.isClass && isInnerClass(
      cls
    )

    if (instanceByName) {
      loggerBySymbolName(cls)(f)
    } else {
      loggerByType(cls)(f)
    }
  }

  /** A macro context that represents a method call on a Logger instance. */
  private[this] type LogCtx[F[_]] = whitebox.Context { type PrefixType = Slf4jLoggerInternal[F] }

  /** Log a message reflectively at a given level.
   *
   * This is the internal workhorse method that does most of the logging for real applications.
   *
   * @param msg the message that the user wants to log
   * @param error the `Throwable` that we're logging along with the message, if any
   * @param logLevel the level of the logging
   */
  private[this] def reflectiveLog[F[_]](
      c: LogCtx[F]
  )(msg: c.Tree, error: Option[c.Expr[Throwable]], context: Seq[c.Expr[(String, String)]])(
      logLevel: LogLevel) = {
    import c.universe._

    val logger = q"${c.prefix.tree}.logger"
    val F = q"${c.prefix}.F"
    val logValues = error match {
      case None => List(msg)
      case Some(e) => List(msg, e.tree)
    }
    val logExpr = q"$logger.${TermName(logLevel.methodName)}(..$logValues)"
    val checkExpr = q"$logger.${TermName(s"is${logLevel.name}Enabled")}"

    def errorIsSimple = {
      error match {
        case None | Some(c.Expr(Ident(_))) => true
        case _ => false
      }
    }

    msg match {
      case _ if context.nonEmpty =>
        val MDC = q"org.slf4j.MDC"
        val Seq = q"scala.collection.Seq"
        val backup = TermName(c.freshName("mdcBackup"))
        q"""
           if ($checkExpr) $F.delay {
             val $backup = $MDC.getCopyOfContextMap
             try {
               for ((k, v) <- $Seq(..$context)) $MDC.put(k, v)
               $logExpr
             } finally {
               if ($backup eq null) $MDC.clear()
               else $MDC.setContextMap($backup)
             }
           } else $F.unit
         """
      case c.Expr(Literal(Constant(_))) if errorIsSimple =>
        q"$F.delay($logExpr)"
      case _ =>
        q"if ($checkExpr) $F.delay($logExpr) else $F.unit"
    }
  }

  def traceTM[F[_]](c: LogCtx[F])(t: c.Expr[Throwable])(msg: c.Tree) =
    reflectiveLog(c)(msg, Some(t), Nil)(LogLevel.Trace)

  def traceM[F[_]](c: LogCtx[F])(msg: c.Tree) = reflectiveLog(c)(msg, None, Nil)(LogLevel.Trace)

  def traceCM[F[_]](c: LogCtx[F])(ctx: c.Expr[(String, String)]*)(msg: c.Tree) =
    reflectiveLog(c)(msg, None, ctx)(LogLevel.Trace)

  def debugTM[F[_]](c: LogCtx[F])(t: c.Expr[Throwable])(msg: c.Tree) =
    reflectiveLog(c)(msg, Some(t), Nil)(LogLevel.Debug)

  def debugM[F[_]](c: LogCtx[F])(msg: c.Tree) = reflectiveLog(c)(msg, None, Nil)(LogLevel.Debug)

  def debugCM[F[_]](c: LogCtx[F])(ctx: c.Expr[(String, String)]*)(msg: c.Tree) =
    reflectiveLog(c)(msg, None, ctx)(LogLevel.Debug)

  def infoTM[F[_]](c: LogCtx[F])(t: c.Expr[Throwable])(msg: c.Tree) =
    reflectiveLog(c)(msg, Some(t), Nil)(LogLevel.Info)

  def infoM[F[_]](c: LogCtx[F])(msg: c.Tree) = reflectiveLog(c)(msg, None, Nil)(LogLevel.Info)

  def infoCM[F[_]](c: LogCtx[F])(ctx: c.Expr[(String, String)]*)(msg: c.Tree) =
    reflectiveLog(c)(msg, None, ctx)(LogLevel.Info)

  def warnTM[F[_]](c: LogCtx[F])(t: c.Expr[Throwable])(msg: c.Tree) =
    reflectiveLog(c)(msg, Some(t), Nil)(LogLevel.Warn)

  def warnM[F[_]](c: LogCtx[F])(msg: c.Tree) = reflectiveLog(c)(msg, None, Nil)(LogLevel.Warn)

  def warnCM[F[_]](c: LogCtx[F])(ctx: c.Expr[(String, String)]*)(msg: c.Tree) =
    reflectiveLog(c)(msg, None, ctx)(LogLevel.Warn)

  def errorTM[F[_]](c: LogCtx[F])(t: c.Expr[Throwable])(msg: c.Tree) =
    reflectiveLog(c)(msg, Some(t), Nil)(LogLevel.Error)

  def errorM[F[_]](c: LogCtx[F])(msg: c.Tree) = reflectiveLog(c)(msg, None, Nil)(LogLevel.Error)

  def errorCM[F[_]](c: LogCtx[F])(ctx: c.Expr[(String, String)]*)(msg: c.Tree) =
    reflectiveLog(c)(msg, None, ctx)(LogLevel.Error)
}
