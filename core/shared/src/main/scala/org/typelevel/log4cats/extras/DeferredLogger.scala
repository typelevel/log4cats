package org.typelevel.log4cats.extras

import cats.data.Chain
import cats.effect.kernel.Resource.ExitCase
import cats.effect.kernel.{Concurrent, Ref, Resource}
import cats.syntax.all.*
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.extras.DeferredLogger.DeferredLogMessage

/**
 * `Logger` that does not immediately log.
 *
 * Similar in idea to `WriterLogger`, but a bit safer. This will not lose logs when the effect
 * fails, instead logging when the resource is cancelled or fails.
 *
 * This can be used to implement failure-only logging.
 * {{{
 *   def handleRequest[F[_](request: Request[F], logger: StructuredLogger[F]): OptionT[F, Response[F]] = ???
 *
 *   HttpRoutes[F] { req =>
 *     DeferredLogger[F](logger)
 *       .mapK(OptionT.liftK[F])
 *       .use { logger =>
 *         handleRequest(request, deferredLogger).flatTap { response =>
 *           deferredLogger.log.unlessA(response.status.isSuccess)
 *         }
 *       }
 *   }
 * }}}
 */
trait DeferredLogger[F[_]] extends Logger[F] {
  def inspect: F[Chain[DeferredLogMessage]]
  def log: F[Unit]
}
object DeferredLogger {
  def apply[F[_]](logger: Logger[F])(implicit F: Concurrent[F]): Resource[F, DeferredLogger[F]] =
    Resource
      .makeCase(Ref.empty[F, Chain[DeferredLogMessage]]) { (ref, exitCase) =>
        exitCase match {
          case ExitCase.Succeeded => F.unit
          case _ => ref.get.flatMap(_.traverse_(_.log(logger)))
        }
      }
      .map { ref =>
        new DeferredLogger[F] {
          private def save(lm: DeferredLogMessage): F[Unit] = ref.update(_.append(lm))

          override def trace(t: Throwable)(msg: => String): F[Unit] = save(Trace(() => msg, t.some))
          override def debug(t: Throwable)(msg: => String): F[Unit] = save(Debug(() => msg, t.some))
          override def info(t: Throwable)(msg: => String): F[Unit] = save(Info(() => msg, t.some))
          override def warn(t: Throwable)(msg: => String): F[Unit] = save(Warn(() => msg, t.some))
          override def error(t: Throwable)(msg: => String): F[Unit] = save(Error(() => msg, t.some))

          override def trace(msg: => String): F[Unit] = save(Trace(() => msg, none))
          override def debug(msg: => String): F[Unit] = save(Debug(() => msg, none))
          override def info(msg: => String): F[Unit] = save(Info(() => msg, none))
          override def warn(msg: => String): F[Unit] = save(Warn(() => msg, none))
          override def error(msg: => String): F[Unit] = save(Error(() => msg, none))

          override def inspect: F[Chain[DeferredLogMessage]] = ref.get

          override def log: F[Unit] = ref.getAndSet(Chain.empty).flatMap(_.traverse_(_.log(logger)))
        }
      }

  sealed trait DeferredLogMessage {
    def message: () => String
    def throwOpt: Option[Throwable]

    def log[F[_]](logger: Logger[F]): F[Unit] = this match {
      case Trace(message, Some(e)) => logger.trace(e)(message())
      case Trace(message, None) => logger.trace(message())
      case Debug(message, Some(e)) => logger.debug(e)(message())
      case Debug(message, None) => logger.debug(message())
      case Info(message, Some(e)) => logger.info(e)(message())
      case Info(message, None) => logger.info(message())
      case Warn(message, Some(e)) => logger.warn(e)(message())
      case Warn(message, None) => logger.warn(message())
      case Error(message, Some(e)) => logger.error(e)(message())
      case Error(message, None) => logger.error(message())
    }
  }

  final case class Trace(
      message: () => String,
      throwOpt: Option[Throwable]
  ) extends DeferredLogMessage
  final case class Debug(
      message: () => String,
      throwOpt: Option[Throwable]
  ) extends DeferredLogMessage
  final case class Info(
      message: () => String,
      throwOpt: Option[Throwable]
  ) extends DeferredLogMessage
  final case class Warn(
      message: () => String,
      throwOpt: Option[Throwable]
  ) extends DeferredLogMessage
  final case class Error(
      message: () => String,
      throwOpt: Option[Throwable]
  ) extends DeferredLogMessage
}
