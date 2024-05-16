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

package org.typelevel.log4cats.extras

import cats.data.Chain
import cats.effect.kernel.Resource.ExitCase
import cats.effect.kernel.{Concurrent, Ref, Resource}
import cats.kernel.Hash
import cats.syntax.all.*
import org.typelevel.log4cats.StructuredLogger
import org.typelevel.log4cats.extras.DeferredStructuredLogger.DeferredStructuredLogMessage

/**
 * `StructuredLogger` that does not immediately log.
 *
 * Similar in idea to `WriterStructuredLogger`, but a bit safer. This will not lose logs when the
 * effect fails, instead logging when the resource is cancelled or fails.
 *
 * This can be used to implement failure-only logging.
 * {{{
 *   def handleRequest[F[_](request: Request[F], logger: StructuredLogger[F]): OptionT[F, Response[F]] = ???
 *
 *   HttpRoutes[F] { req =>
 *     DeferredStructuredLogger[F](logger)
 *       .mapK(OptionT.liftK[F])
 *       .use { logger =>
 *         handleRequest(request, deferredLogger).flatTap { response =>
 *           deferredLogger.log.unlessA(response.status.isSuccess)
 *         }
 *       }
 *   }
 * }}}
 *
 * >>> WARNING: READ BEFORE USAGE! <<<
 * https://github.com/typelevel/log4cats/blob/main/core/shared/src/main/scala/org/typelevel/log4cats/extras/README.md
 * >>> WARNING: READ BEFORE USAGE! <<<
 */
trait DeferredStructuredLogger[F[_]] extends StructuredLogger[F] {

  /**
   * View the logs in the buffer.
   *
   * This is primarily useful for testing, and will not effect the behavior of calls to `log`
   */
  def inspect: F[Chain[DeferredStructuredLogMessage]]

  /**
   * Log the deferred messages
   *
   * This may be called multiple times, and each log should only be logged once.
   */
  def log: F[Unit]
}
object DeferredStructuredLogger {
  def apply[F[_]](
      logger: StructuredLogger[F]
  )(implicit F: Concurrent[F]): Resource[F, DeferredStructuredLogger[F]] =
    Resource
      .makeCase(Ref.empty[F, Chain[DeferredStructuredLogMessage]]) { (ref, exitCase) =>
        exitCase match {
          case ExitCase.Succeeded => F.unit
          case _ => ref.get.flatMap(_.traverse_(_.log(logger)))
        }
      }
      .map { ref =>
        new DeferredStructuredLogger[F] {
          private def save(lm: DeferredStructuredLogMessage): F[Unit] = ref.update(_.append(lm))

          override def trace(ctx: Map[String, String])(msg: => String): F[Unit] =
            save(Trace(() => msg, none, ctx))
          override def debug(ctx: Map[String, String])(msg: => String): F[Unit] =
            save(Debug(() => msg, none, ctx))
          override def info(ctx: Map[String, String])(msg: => String): F[Unit] =
            save(Info(() => msg, none, ctx))
          override def warn(ctx: Map[String, String])(msg: => String): F[Unit] =
            save(Warn(() => msg, none, ctx))
          override def error(ctx: Map[String, String])(msg: => String): F[Unit] =
            save(Error(() => msg, none, ctx))

          override def trace(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
            save(Trace(() => msg, t.some, ctx))
          override def debug(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
            save(Debug(() => msg, t.some, ctx))
          override def info(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
            save(Info(() => msg, t.some, ctx))
          override def warn(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
            save(Warn(() => msg, t.some, ctx))
          override def error(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
            save(Error(() => msg, t.some, ctx))

          override def trace(t: Throwable)(msg: => String): F[Unit] =
            save(Trace(() => msg, t.some, Map.empty))
          override def debug(t: Throwable)(msg: => String): F[Unit] =
            save(Debug(() => msg, t.some, Map.empty))
          override def info(t: Throwable)(msg: => String): F[Unit] =
            save(Info(() => msg, t.some, Map.empty))
          override def warn(t: Throwable)(msg: => String): F[Unit] =
            save(Warn(() => msg, t.some, Map.empty))
          override def error(t: Throwable)(msg: => String): F[Unit] =
            save(Error(() => msg, t.some, Map.empty))

          override def trace(msg: => String): F[Unit] = save(Trace(() => msg, none, Map.empty))
          override def debug(msg: => String): F[Unit] = save(Debug(() => msg, none, Map.empty))
          override def info(msg: => String): F[Unit] = save(Info(() => msg, none, Map.empty))
          override def warn(msg: => String): F[Unit] = save(Warn(() => msg, none, Map.empty))
          override def error(msg: => String): F[Unit] = save(Error(() => msg, none, Map.empty))

          override def inspect: F[Chain[DeferredStructuredLogMessage]] = ref.get

          override def log: F[Unit] = ref.getAndSet(Chain.empty).flatMap(_.traverse_(_.log(logger)))
        }
      }

  sealed trait DeferredStructuredLogMessage {
    def ctx: Map[String, String]
    def message: () => String
    def throwOpt: Option[Throwable]

    def log[F[_]](logger: StructuredLogger[F]): F[Unit] = this match {
      case Trace(message, Some(e), ctx) => logger.trace(ctx, e)(message())
      case Trace(message, None, ctx) => logger.trace(ctx)(message())
      case Debug(message, Some(e), ctx) => logger.debug(ctx, e)(message())
      case Debug(message, None, ctx) => logger.debug(ctx)(message())
      case Info(message, Some(e), ctx) => logger.info(ctx, e)(message())
      case Info(message, None, ctx) => logger.info(ctx)(message())
      case Warn(message, Some(e), ctx) => logger.warn(ctx, e)(message())
      case Warn(message, None, ctx) => logger.warn(ctx)(message())
      case Error(message, Some(e), ctx) => logger.error(ctx, e)(message())
      case Error(message, None, ctx) => logger.error(ctx)(message())
    }

    override def equals(obj: Any): Boolean = obj match {
      case other: DeferredStructuredLogMessage => deferredStructuredLogMessageHash.eqv(this, other)
      case _ => false
    }

    override def hashCode(): Int = deferredStructuredLogMessageHash.hash(this)
  }

  final case class Trace(
      message: () => String,
      throwOpt: Option[Throwable],
      ctx: Map[String, String]
  ) extends DeferredStructuredLogMessage
  final case class Debug(
      message: () => String,
      throwOpt: Option[Throwable],
      ctx: Map[String, String]
  ) extends DeferredStructuredLogMessage
  final case class Info(
      message: () => String,
      throwOpt: Option[Throwable],
      ctx: Map[String, String]
  ) extends DeferredStructuredLogMessage
  final case class Warn(
      message: () => String,
      throwOpt: Option[Throwable],
      ctx: Map[String, String]
  ) extends DeferredStructuredLogMessage
  final case class Error(
      message: () => String,
      throwOpt: Option[Throwable],
      ctx: Map[String, String]
  ) extends DeferredStructuredLogMessage

  implicit val deferredStructuredLogMessageHash: Hash[DeferredStructuredLogMessage] = Hash.by {
    case Trace(message, throwOpt, ctx) =>
      (0, message(), throwOpt.map(_.getMessage), ctx)
    case Debug(message, throwOpt, ctx) =>
      (1, message(), throwOpt.map(_.getMessage), ctx)
    case Info(message, throwOpt, ctx) =>
      (2, message(), throwOpt.map(_.getMessage), ctx)
    case Warn(message, throwOpt, ctx) =>
      (3, message(), throwOpt.map(_.getMessage), ctx)
    case Error(message, throwOpt, ctx) =>
      (4, message(), throwOpt.map(_.getMessage), ctx)
  }
}
