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
import io.chrisdavenport.log4cats.Logger

import language.experimental.macros
import org.slf4j.{Logger => JLogger}

object Slf4jLogger {

  def create[F[_]: Sync]: Logger[F] = macro LoggerMacros.getLoggerImpl[F[_]]

  def fromName[F[_]: Sync](name: String): Logger[F] =
    new Slf4jLogger(org.slf4j.LoggerFactory.getLogger(name))

  def fromClass[F[_]: Sync](clazz: Class[_]): Logger[F] =
    new Slf4jLogger(org.slf4j.LoggerFactory.getLogger(clazz))

  final val singletonsByName = true
  final val trailingDollar = false

  sealed trait LevelLogger[F[_]] extends Any {
    def isEnabled: F[Boolean]

    def apply(msg: => String): F[Unit]
    def apply(t: Throwable)(msg: => String): F[Unit]
  }
//
//  final class TraceLevelLogger[F[_]: Sync] private[slf4j] (val logger: JLogger)
//      extends AnyVal
//      with LevelLogger[F] {
//    @inline def isEnabled: F[Boolean] = F.delay(logger.isTraceEnabled)
//    @inline def apply(msg: => String): F[Unit] = isEnabled.flatMap {isEnabled => if(isEnabled) $F.delay($logExpr) else $F.unit
//    @inline def apply(t: Throwable)(msg: => String) = if (isEnabled) logger.trace(msg, t)
//  }

//  final class DebugLevelLogger private[slf4j] (val logger: JLogger)
//      extends AnyVal
//      with LevelLogger {
//    @inline def isEnabled: Boolean = logger.isDebugEnabled
//    @inline def apply(msg: => String): Unit = if (isEnabled) logger.debug(msg)
//    @inline def apply(t: Throwable)(msg: => String): Unit = if (isEnabled) logger.debug(msg, t)
//  }
//
//  final class InfoLevelLogger private[slf4j] (val logger: JLogger)
//      extends AnyVal
//      with LevelLogger {
//    @inline def isEnabled: Boolean = logger.isInfoEnabled
//    @inline def apply(msg: => String): Unit = if (isEnabled) logger.info(msg)
//    @inline def apply(t: Throwable)(msg: => String): Unit = if (isEnabled) logger.info(msg, t)
//  }
//
//  final class WarnLevelLogger private[slf4j] (val logger: JLogger)
//      extends AnyVal
//      with LevelLogger {
//    @inline def isEnabled: Boolean = logger.isWarnEnabled
//    @inline def apply(msg: => String): Unit = if (isEnabled) logger.warn(msg)
//    @inline def apply(t: Throwable)(msg: => String): Unit = if (isEnabled) logger.warn(msg, t)
//  }
//
//  final class ErrorLevelLogger private[slf4j] (val logger: JLogger)
//      extends AnyVal
//      with LevelLogger {
//    @inline def isEnabled: Boolean = logger.isErrorEnabled
//    @inline def apply(msg: => String): Unit = if (isEnabled) logger.error(msg)
//    @inline def apply(t: Throwable)(msg: => String): Unit = if (isEnabled) logger.error(msg, t)
//  }

  /**
    * The existance of this class is due to the in-ability of macros to
    * override abstract methods, only methods directly.
    *
    * See:
    * https://github.com/scala/scala/commit/ef979c02da887b7c56bc1da9c4eb888e92af570f
    */
  private[Slf4jLogger] class Slf4jLoggerDummyMacro[F[_]] extends Logger[F] {

    @inline override def isTraceEnabled: F[Boolean] = ???

    @inline override def isDebugEnabled: F[Boolean] = ???

    @inline override def isInfoEnabled: F[Boolean] = ???

    @inline override def isWarnEnabled: F[Boolean] = ???

    @inline override def isErrorEnabled: F[Boolean] = ???

    override def trace(t: Throwable)(msg: => String): F[Unit] = ???
    override def trace(msg: => String): F[Unit] = ???
    def trace(ctx: (String, String)*)(msg: => String): F[Unit] = ???

    override def debug(t: Throwable)(msg: => String): F[Unit] = ???
    override def debug(msg: => String): F[Unit] = ???
    def debug(ctx: (String, String)*)(msg: => String): F[Unit] = ???

    override def info(t: Throwable)(msg: => String): F[Unit] = ???
    override def info(msg: => String): F[Unit] = ???
    def info(ctx: (String, String)*)(msg: => String): F[Unit] = ???

    override def warn(t: Throwable)(msg: => String): F[Unit] = ???
    override def warn(msg: => String): F[Unit] = ???
    def warn(ctx: (String, String)*)(msg: => String): F[Unit] = ???

    override def error(t: Throwable)(msg: => String): F[Unit] = ???
    override def error(msg: => String): F[Unit] = ???
    def error(ctx: (String, String)*)(msg: => String): F[Unit] = ???
  }
}

final class Slf4jLogger[F[_]: Sync](val logger: JLogger) extends Slf4jLogger.Slf4jLoggerDummyMacro[F] {
  val F: Sync[F] = Sync[F]
  @inline override def isTraceEnabled: F[Boolean] = F.delay(logger.isTraceEnabled)

  @inline override def isDebugEnabled: F[Boolean] = F.delay(logger.isDebugEnabled)

  @inline override def isInfoEnabled: F[Boolean] = F.delay(logger.isInfoEnabled)

  @inline override def isWarnEnabled: F[Boolean] = F.delay(logger.isWarnEnabled)

  @inline override def isErrorEnabled: F[Boolean] = F.delay(logger.isErrorEnabled)

  import LoggerMacros._

  override def trace(t: Throwable)(msg: => String): F[Unit] = macro traceTM[F]
  override def trace(msg: => String): F[Unit] = macro traceM[F]
  override def trace(ctx: (String, String)*)(msg: => String): F[Unit] = macro traceCM[F]

  override def debug(t: Throwable)(msg: => String): F[Unit] = macro debugTM[F]
  override def debug(msg: => String): F[Unit] = macro debugM[F]
  override def debug(ctx: (String, String)*)(msg: => String): F[Unit] = macro debugCM[F]

  override def info(t: Throwable)(msg: => String): F[Unit] = macro infoTM[F]
  override def info(msg: => String): F[Unit] = macro infoM[F]
  override def info(ctx: (String, String)*)(msg: => String): F[Unit] = macro infoCM[F]

  override def warn(t: Throwable)(msg: => String): F[Unit] = macro warnTM[F]
  override def warn(msg: => String): F[Unit] = macro warnM[F]
  override def warn(ctx: (String, String)*)(msg: => String): F[Unit] = macro warnCM[F]

  override def error(t: Throwable)(msg: => String): F[Unit] = macro errorTM[F]
  override def error(msg: => String): F[Unit] = macro errorM[F]
  override def error(ctx: (String, String)*)(msg: => String): F[Unit] = macro errorCM[F]

}

