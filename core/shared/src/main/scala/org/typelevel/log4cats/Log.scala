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

    def build(): Log[Ctx]
  }

  def mutableBuilder[Ctx](): Builder[Ctx] = new MutableBuilder[Ctx]()

  private val noopMessage: () => String = () => ""

  private class MutableBuilder[Ctx] extends Builder[Ctx] {
    private var _timestamp: Option[FiniteDuration] = None
    private var _level: Option[KernelLogLevel] = None
    private var _message: () => String = noopMessage
    private var _throwable: Option[Throwable] = None
    private val _context: mutable.Map[String, Ctx] = mutable.Map.empty[String, Ctx]
    private var _fileName: Option[String] = None
    private var _className: Option[String] = None
    private var _methodName: Option[String] = None
    private var _line: Option[Int] = None

    def build(): Log[Ctx] = new Log[Ctx] {
      override def timestamp: Option[FiniteDuration] = _timestamp
      override def level: KernelLogLevel = _level.getOrElse(KernelLogLevel.Info)
      override def levelValue: Int = level.value
      override def message: String = _message()
      override def throwable: Option[Throwable] = _throwable
      override def context: Map[String, Ctx] = _context.toMap
      override def className: Option[String] = _className
      override def fileName: Option[String] = _fileName
      override def methodName: Option[String] = _methodName
      override def line: Option[Int] = _line.filter(_ > 0)
      override def unsafeThrowable: Throwable = _throwable.get
      override def unsafeContext: Map[String, Ctx] = _context.toMap
    }

    override def withTimestamp(value: FiniteDuration): this.type = {
      _timestamp = Some(value)
      this
    }

    override def withLevel(level: KernelLogLevel): this.type = {
      _level = Some(level)
      this
    }

    override def withMessage(message: => String): this.type = {
      _message = () => message
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
