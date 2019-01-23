package io.chrisdavenport.log4cats.slf4j.internal

import io.chrisdavenport.log4cats._
import cats.implicits._
import cats.effect._
import org.slf4j.{Logger => JLogger}
import org.slf4j.MDC

private[slf4j] object Slf4jLoggerInternal {

  final val singletonsByName = true
  final val trailingDollar = false

  sealed trait LevelLogger[F[_]] extends Any {
    def isEnabled: F[Boolean]

    def apply(msg: => String): F[Unit]
    def apply(t: Throwable)(msg: => String): F[Unit]
  }

  final class Slf4jLogger[F[_]](val logger: JLogger)(implicit F: Sync[F]) extends SelfAwareStructuredLogger[F] {
      override def isTraceEnabled: F[Boolean] = F.delay(logger.isTraceEnabled)
      override def isDebugEnabled: F[Boolean] = F.delay(logger.isDebugEnabled)
      override def isInfoEnabled: F[Boolean] = F.delay(logger.isInfoEnabled)
      override def isWarnEnabled: F[Boolean] = F.delay(logger.isWarnEnabled)
      override def isErrorEnabled: F[Boolean] = F.delay(logger.isErrorEnabled)

      override def trace(t: Throwable)(msg: => String): F[Unit] = F.suspend{
        isTraceEnabled
        .ifM(F.delay(logger.trace(msg, t)), F.unit)
      }
      override def trace(msg: => String): F[Unit] = 
        isTraceEnabled
          .ifM(F.delay(logger.trace(msg)), F.unit)
      override def trace(ctx: Map[String, String])(msg: => String): F[Unit] = {
        isTraceEnabled.ifM( F.delay {
          val backup = MDC.getCopyOfContextMap
          try {
            for {
              (k, v) <- ctx
            } MDC.put(k, v)
            logger.trace(msg)
          } finally {
            if (backup eq null) MDC.clear()
            else MDC.setContextMap(backup)
          }
        }, F.unit
        )
      }
      override def debug(t: Throwable)(msg: => String): F[Unit] =
        isDebugEnabled
        .ifM(F.delay(logger.debug(msg, t)), F.unit)
      override def debug(msg: => String): F[Unit] =
        isDebugEnabled
        .ifM(F.delay(logger.debug(msg)), F.unit)
      override def debug(ctx: Map[String, String])(msg: => String): F[Unit] =
        isDebugEnabled.ifM( F.delay {
          val backup = MDC.getCopyOfContextMap
          try {
            for {
              (k, v) <- ctx
            } MDC.put(k, v)
            logger.debug(msg)
          } finally {
            if (backup eq null) MDC.clear()
            else MDC.setContextMap(backup)
          }
        }, F.unit)
      override def info(t: Throwable)(msg: => String): F[Unit] = 
        isInfoEnabled
        .ifM(F.delay(logger.info(msg,t)), F.unit)
      override def info(msg: => String): F[Unit] = 
        isInfoEnabled
        .ifM(F.delay(logger.info(msg)),F.unit)
      override def info(ctx: Map[String, String])(msg: => String): F[Unit] = 
        isInfoEnabled.ifM(F.delay {
          val backup = MDC.getCopyOfContextMap
          try {
            for {
              (k, v) <- ctx
            } MDC.put(k, v)
            logger.info(msg)
          } finally {
            if (backup eq null) MDC.clear()
            else MDC.setContextMap(backup)
          }
        }, F.unit)
      override def warn(t: Throwable)(msg: => String): F[Unit] = 
        isWarnEnabled
        .ifM(F.delay(logger.warn(msg,t)), F.unit)
      override def warn(msg: => String): F[Unit] = 
        isWarnEnabled
        .ifM(F.delay(logger.warn(msg)), F.unit)
      override def warn(ctx: Map[String, String])(msg: => String): F[Unit] =
        isWarnEnabled.ifM(F.delay {
          val backup = MDC.getCopyOfContextMap
          try {
            for {
              (k, v) <- ctx
            } MDC.put(k, v)
            logger.warn(msg)
          } finally {
            if (backup eq null) MDC.clear()
            else MDC.setContextMap(backup)
          }
        }, F.unit)
      override def error(t: Throwable)(msg: => String): F[Unit] = 
        isErrorEnabled
        .ifM(F.delay(logger.error(msg,t)),F.unit)
      override def error(msg: => String): F[Unit] =
        isErrorEnabled
        .ifM(F.delay(logger.error(msg)), F.unit)
      override def error(ctx: Map[String, String])(msg: => String): F[Unit] = 
        isErrorEnabled.ifM(F.delay {
          val backup = MDC.getCopyOfContextMap
          try {
            for {
              (k, v) <- ctx
            } MDC.put(k, v)
            logger.error(msg)
          } finally {
            if (backup eq null) MDC.clear()
            else MDC.setContextMap(backup)
          }
        }, F.unit)
      override def trace(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
        isTraceEnabled.ifM(F.delay {
          val backup = MDC.getCopyOfContextMap
          try {
            for {
              (k, v) <- ctx
            } MDC.put(k, v)
            logger.trace(msg, t)
          } finally {
            if (backup eq null) MDC.clear()
            else MDC.setContextMap(backup)
          }
        }, F.unit)
      override def debug(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
        isDebugEnabled.ifM(F.delay {
          val backup = MDC.getCopyOfContextMap
          try {
            for {
              (k, v) <- ctx
            } MDC.put(k, v)
            logger.debug(msg, t)
          } finally {
            if (backup eq null) MDC.clear()
            else MDC.setContextMap(backup)
          }
        } , F.unit)
      override def info(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
        isInfoEnabled.ifM(F.delay {
          val backup = MDC.getCopyOfContextMap
          try {
            for {
              (k, v) <- ctx
            } MDC.put(k, v)
            logger.info(msg, t)
          } finally {
            if (backup eq null) MDC.clear()
            else MDC.setContextMap(backup)
          }
        }, F.unit)
      override def warn(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
        isWarnEnabled.ifM(F.delay {
          val backup = MDC.getCopyOfContextMap
          try {
            for {
              (k, v) <- ctx
            } MDC.put(k, v)
            logger.warn(msg, t)
          } finally {
            if (backup eq null) MDC.clear()
            else MDC.setContextMap(backup)
          }
        }, F.unit)
      override def error(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
        isErrorEnabled.ifM(F.delay {
          val backup = MDC.getCopyOfContextMap
          try {
            for {
              (k, v) <- ctx
            } MDC.put(k, v)
            logger.error(msg,t)
          } finally {
            if (backup eq null) MDC.clear()
            else MDC.setContextMap(backup)
          }
        }, F.unit)
    }
}
