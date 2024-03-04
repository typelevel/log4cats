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

import org.typelevel.log4cats.internal.SharedLoggerNameMacro

import scala.reflect.macros.blackbox

/**
 * Macros that support the logging system.
 *
 * See for handling call-by-name-parameters in macros https://issues.scala-lang.org/browse/SI-5778
 *
 * @author
 *   Sarah Gerweck <sarah@atscale.com>
 */
private[slf4j] class GetLoggerMacros(val c: blackbox.Context) {
  final def safeCreateImpl[F](f: c.Expr[F]) = getLoggerImpl[F](f, delayed = true)

  final def unsafeCreateImpl[F](f: c.Expr[F]) = getLoggerImpl[F](f, delayed = false)

  private def getLoggerImpl[F](f: c.Expr[F], delayed: Boolean) = {
    val loggerName = SharedLoggerNameMacro.getLoggerNameImpl(c)
    import c.universe._

    def loggerByParam(param: c.Tree) = {
      val unsafeCreate =
        q"_root_.org.typelevel.log4cats.slf4j.Slf4jLogger.getLoggerFromSlf4j(_root_.org.slf4j.LoggerFactory.getLogger(...${List(param)}))($f)"
      if (delayed)
        q"_root_.cats.effect.Sync.apply($f).delay(...$unsafeCreate)"
      else
        unsafeCreate
    }
    loggerByParam(q"$loggerName.value")
  }
}
