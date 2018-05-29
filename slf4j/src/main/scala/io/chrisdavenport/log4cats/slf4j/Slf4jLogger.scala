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
package io.chrisdavenport.log4cats.slf4j

import cats.effect.Sync
import io.chrisdavenport.log4cats.{Logger, LogLevelAware, MDCLogger}
import io.chrisdavenport.log4cats.slf4j.internal._
import org.slf4j.{Logger => JLogger}

import language.experimental.macros

object Slf4jLogger {

  def create[F[_]: Sync]: Logger[F] with LogLevelAware[F] with MDCLogger[F] = 
    macro LoggerMacros.getLoggerImpl[F[_]]

  def fromName[F[_]: Sync](name: String):Logger[F] with LogLevelAware[F] with MDCLogger[F] =
    fromSlf4jLogger(new Slf4jLoggerInternal[F](org.slf4j.LoggerFactory.getLogger(name)))

  def fromClass[F[_]: Sync](clazz: Class[_]): Logger[F] with LogLevelAware[F] with MDCLogger[F] =
    fromSlf4jLogger(new Slf4jLoggerInternal[F](org.slf4j.LoggerFactory.getLogger(clazz)))

  def fromSlf4j[F[_]: Sync](s: JLogger): Logger[F] with LogLevelAware[F] with MDCLogger[F] =
    fromSlf4jLogger(new Slf4jLoggerInternal[F](s))

  private def fromSlf4jLogger[F[_]: Sync](s: Slf4jLoggerInternal[F]): Logger[F] with LogLevelAware[F] with MDCLogger[F] = 
    new Logger[F] with LogLevelAware[F] with MDCLogger[F] {
      @inline override def isTraceEnabled: F[Boolean] = s.isTraceEnabled
      @inline override def isDebugEnabled: F[Boolean] = s.isDebugEnabled
      @inline override def isInfoEnabled: F[Boolean] = s.isInfoEnabled
      @inline override def isWarnEnabled: F[Boolean] = s.isWarnEnabled
      @inline override def isErrorEnabled: F[Boolean] = s.isErrorEnabled

      @inline override def trace(t: Throwable)(msg: => String): F[Unit] = s.internalTraceTM(t)(msg)
      @inline override def trace(msg: => String): F[Unit] = s.internalTraceM(msg)
      @inline override def trace(ctx: (String, String)*)(msg: => String): F[Unit] = s.internalTraceMDC(ctx:_*)(msg)
      @inline override def debug(t: Throwable)(msg: => String): F[Unit] = s.internalDebugTM(t)(msg)
      @inline override def debug(msg: => String): F[Unit] = s.internalDebugM(msg)
      @inline override def debug(ctx: (String, String)*)(msg: => String): F[Unit] = s.internalDebugMDC(ctx:_*)(msg)
      @inline override def info(t: Throwable)(msg: => String): F[Unit] = s.internalInfoTM(t)(msg)
      @inline override def info(msg: => String): F[Unit] = s.internalInfoM(msg)
      @inline override def info(ctx: (String, String)*)(msg: => String): F[Unit] = s.internalInfoMDC(ctx:_*)(msg)
      @inline override def warn(t: Throwable)(msg: => String): F[Unit] = s.internalWarnTM(t)(msg)
      @inline override def warn(msg: => String): F[Unit] = s.internalWarnM(msg)
      @inline override def warn(ctx: (String, String)*)(msg: => String): F[Unit] = s.internalWarnMDC(ctx:_*)(msg)
      @inline override def error(t: Throwable)(msg: => String): F[Unit] = s.internalErrorTM(t)(msg)
      @inline override def error(msg: => String): F[Unit] = s.internalErrorM(msg)
      @inline override def error(ctx: (String, String)*)(msg: => String): F[Unit] = s.internalErrorMDC(ctx:_*)(msg)
    }


}

