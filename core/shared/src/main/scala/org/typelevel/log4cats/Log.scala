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
  def message: () => String
  def throwable: Option[Throwable]
  def context: Map[String, Ctx]
  def fileName: Option[String]
  def className: Option[String]
  def methodName: Option[String]
  def line: Option[Int]
  def levelValue: Int
}

object Log {
  trait Builder[Ctx] {
    def withTimestamp(value: FiniteDuration): Builder[Ctx]
    def withLevel(level: KernelLogLevel): Builder[Ctx]
    def withMessage(message: => String): Builder[Ctx]
    def withThrowable(throwable: Throwable): Builder[Ctx]
    def withContext[A](name: String)(ctx: A)(implicit E: Context.Encoder[A, Ctx]): Builder[Ctx]
    def withFileName(name: String): Builder[Ctx]
    def withClassName(name: String): Builder[Ctx]
    def withMethodName(name: String): Builder[Ctx]
    def withLine(line: Int): Builder[Ctx]

    final def withContextMap[A](
        contextMap: Map[String, A]
    )(implicit E: Context.Encoder[A, Ctx]): Builder[Ctx] =
      contextMap.foldLeft(this) { case (builder, (k, v)) => builder.withContext(k)(v) }

    def adaptTimestamp(f: FiniteDuration => FiniteDuration): Builder[Ctx]
    def adaptLevel(f: KernelLogLevel => KernelLogLevel): Builder[Ctx]
    def adaptMessage(f: String => String): Builder[Ctx]
    def adaptThrowable(f: Throwable => Throwable): Builder[Ctx]
    def adaptContext(f: Map[String, Ctx] => Map[String, Ctx]): Builder[Ctx]
    def adaptFileName(f: String => String): Builder[Ctx]
    def adaptClassName(f: String => String): Builder[Ctx]
    def adaptMethodName(f: String => String): Builder[Ctx]
    def adaptLine(f: Int => Int): Builder[Ctx]

    def build(): Log[Ctx]
  }

  def mutableBuilder[Ctx](): Builder[Ctx] = new MutableBuilder[Ctx]()

  private class MutableBuilder[Ctx] extends Builder[Ctx] {
    private var _timestamp: Option[FiniteDuration] = None
    private var _level: KernelLogLevel = KernelLogLevel.Info
    private var _message: () => String = () => ""
    private var _throwable: Option[Throwable] = None
    private var _context: mutable.Builder[(String, Ctx), Map[String, Ctx]] = Map.newBuilder[String, Ctx]
    private var _fileName: Option[String] = None
    private var _className: Option[String] = None
    private var _methodName: Option[String] = None
    private var _line: Option[Int] = None

    def build(): Log[Ctx] = new Log[Ctx] {
      override def timestamp: Option[FiniteDuration] = _timestamp
      override def level: KernelLogLevel = _level
      override def message: () => String = _message
      override def throwable: Option[Throwable] = _throwable
      override def context: Map[String, Ctx] = _context.result()
      override def className: Option[String] = _className
      override def fileName: Option[String] = _fileName
      override def methodName: Option[String] = _methodName
      override def line: Option[Int] = _line.filter(_ > 0)
      override def levelValue: Int = _level.value
    }

    override def withTimestamp(value: FiniteDuration): this.type = {
      _timestamp = Some(value)
      this
    }

    override def withLevel(level: KernelLogLevel): this.type = {
      _level = level
      this
    }

    override def withMessage(message: => String): this.type = {
      _message = () => message
      this
    }

    override def adaptMessage(f: String => String): this.type = {
      _message = () => f(_message())
      this
    }

    override def adaptTimestamp(f: FiniteDuration => FiniteDuration): this.type = {
      _timestamp = _timestamp.map(f)
      this
    }

    override def adaptLevel(f: KernelLogLevel => KernelLogLevel): this.type = {
      _level = f(_level)
      this
    }

    override def adaptThrowable(f: Throwable => Throwable): this.type = {
      _throwable = _throwable.map(f)
      this
    }

    override def adaptContext(f: Map[String, Ctx] => Map[String, Ctx]): this.type = {
      val currentContext = _context.result()
      _context = Map.newBuilder[String, Ctx]
      _context.addAll(f(currentContext))
      this
    }

    override def adaptFileName(f: String => String): this.type = {
      _fileName = _fileName.map(f)
      this
    }

    override def adaptClassName(f: String => String): this.type = {
      _className = _className.map(f)
      this
    }

    override def adaptMethodName(f: String => String): this.type = {
      _methodName = _methodName.map(f)
      this
    }

    override def adaptLine(f: Int => Int): this.type = {
      _line = _line.map(f)
      this
    }

    override def withThrowable(throwable: Throwable): this.type = {
      _throwable = Some(throwable)
      this
    }

    override def withContext[A](
        name: String
    )(ctx: A)(implicit E: Context.Encoder[A, Ctx]): this.type = {
      _context += (name -> E.encode(ctx))
      this
    }

    override def withFileName(name: String): this.type = {
      _fileName = Some(name)
      this
    }

    override def withClassName(name: String): this.type = {
      _className = Some(name)
      this
    }

    override def withMethodName(name: String): this.type = {
      _methodName = Some(name)
      this
    }

    override def withLine(line: Int): this.type = {
      _line = if (line > 0) Some(line) else None
      this
    }
  }
}
