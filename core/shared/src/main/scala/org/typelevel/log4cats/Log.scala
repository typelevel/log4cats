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

import scala.collection.mutable
import scala.concurrent.duration.FiniteDuration

/**
 * Low-level interface exposing methods to enrich a log record with relevant information. The
 * methods are designed to capture elements that cannot be easily captured from a monadic context
 * (or by running an effect). Elements such as timestamps should be provided by means of
 * middlewares.
 */
trait Log[Ctx] {
  def timestamp: Option[FiniteDuration]
  def level: KernelLogLevel
  def levelValue: Int
  def message: String
  def throwable: Option[Throwable]
  def context: Map[String, Ctx]
  def fileName: Option[String]
  def className: Option[String]
  def methodName: Option[String]
  def line: Option[Int]

  def unsafeThrowable: Throwable
  def unsafeContext: Map[String, Ctx]
}

object Log {
  trait Builder[Ctx] {
    def withTimestamp(value: FiniteDuration): Builder[Ctx]
    def withLevel(level: KernelLogLevel): Builder[Ctx]
    def withLevelValue(levelValue: Int): Builder[Ctx]
    def withMessage(message: => String): Builder[Ctx]
    def withThrowable(throwable: Throwable): Builder[Ctx]
    def withContext(name: String)(f: Ctx): Builder[Ctx]
    def withFileName(name: String): Builder[Ctx]
    def withClassName(name: String): Builder[Ctx]
    def withLine(line: Int): Builder[Ctx]

    final def withContextMap[A](
        contextMap: Map[String, A]
    )(implicit ev: A =:= Ctx): Builder[Ctx] = {
      var builder = this
      contextMap.foreach { case (k, v) =>
        builder = withContext(k)(ev(v))
      }
      builder
    }

    def build(): Log[Ctx]
  }

  def mutableBuilder[Ctx](): Builder[Ctx] = new MutableBuilder[Ctx]()

  private class MutableBuilder[Ctx] private[Log] () extends Builder[Ctx] with Log[Ctx] {
    private var _timestamp: Option[FiniteDuration] = None
    private var _level: Option[KernelLogLevel] = None
    private var _levelValue: Option[Int] = None
    private var _message: Option[String] = None
    private var _throwable: Option[Throwable] = None
    private var _context: Option[mutable.Map[String, Ctx]] = None
    private var _fileName: Option[String] = None
    private var _className: Option[String] = None
    private var _methodName: Option[String] = None
    private var _line: Option[Int] = None

    def build(): Log[Ctx] = this

    def timestamp: Option[FiniteDuration] = _timestamp
    def level: KernelLogLevel = _level.getOrElse(KernelLogLevel.Debug)
    def levelValue: Int =
      _levelValue.getOrElse(level.value)
    def message: String = _message.getOrElse("")
    def throwable: Option[Throwable] = _throwable
    def context: Map[String, Ctx] =
      _context.map(_.toMap).getOrElse(Map.empty)

    def className: Option[String] = _className
    def fileName: Option[String] = _fileName
    def methodName: Option[String] = _methodName
    def line: Option[Int] = _line.filter(_ > 0)

    def unsafeThrowable: Throwable = _throwable.get
    def unsafeContext: Map[String, Ctx] = _context.get.toMap

    def withTimestamp(value: FiniteDuration): this.type = {
      this._timestamp = Some(value)
      this
    }

    def withLevel(level: KernelLogLevel): this.type = {
      this._level = Some(level)
      this
    }

    def withLevelValue(levelValue: Int): this.type = {
      this._levelValue = Some(levelValue)
      this
    }

    def withMessage(message: => String): this.type = {
      this._message = Some(message)
      this
    }

    def withThrowable(throwable: Throwable): this.type = {
      this._throwable = Some(throwable)
      this
    }

    def withContext(name: String)(value: Ctx): this.type = {
      val map = _context.getOrElse {
        val newMap = mutable.Map.empty[String, Ctx]
        this._context = Some(newMap)
        newMap
      }
      map += name -> value
      this
    }

    def withFileName(name: String): this.type = {
      this._fileName = Some(name)
      this
    }

    def withClassName(name: String): this.type = {
      this._className = Some(name)
      this
    }

    def withLine(line: Int): this.type = {
      this._line = Some(line)
      this
    }

    def withMethodName(name: String): this.type = {
      this._methodName = Some(name)
      this
    }
  }
}
