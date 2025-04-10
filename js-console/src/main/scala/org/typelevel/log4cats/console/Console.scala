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

/* All documentation for facades is thanks to Mozilla Contributors at https://developer.mozilla.org/en-US/docs/Web/API
 * and available under the Creative Commons Attribution-ShareAlike v2.5 or later.
 * http://creativecommons.org/licenses/by-sa/2.5/
 *
 * Everything else is under the MIT License http://opensource.org/licenses/MIT
 */

package org.typelevel.log4cats.console

import org.typelevel.scalaccompat.annotation.*

import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal

/**
 * The console object provides access to the browser's debugging console. The specifics of how it
 * works vary from browser to browser, but there is a de facto set of features that are typically
 * provided.
 *
 * There are more methods available; this is the minimal façade needed for console logging from
 * log4cats.
 */
@js.native
@JSGlobal("console")
@nowarn212("cat=unused")
private[console] object Console extends js.Object {

  /**
   * Outputs an informational message to the Web Console. In Firefox, a small "i" icon is displayed
   * next to these items in the Web Console's log.
   */
  def info(message: Any, optionalParams: Any*): Unit = js.native

  /**
   * Outputs a warning message. You may use string substitution and additional arguments with this
   * method. See Using string substitutions.
   */
  def warn(message: Any, optionalParams: Any*): Unit = js.native

  /**
   * Outputs an error message. You may use string substitution and additional arguments with this
   * method. See Using string substitutions.
   */
  def error(message: Any, optionalParams: Any*): Unit = js.native

  /**
   * Outputs a debug message. You may use string substitution and additional arguments with this
   * method. See Using string substitutions.
   */
  def debug(message: Any, optionalParams: Any*): Unit = js.native
}
