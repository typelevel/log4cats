package org.typelevel.log4cats

import cats.Monad
import cats.mtl.Ask

private[log4cats] class AskStructuredLogger[F[_]](logger: StructuredLogger[F])(implicit
    F: Monad[F],
    ask: Ask[F, Map[String, String]]
) extends StructuredLogger[F] {

  override def error(message: => String): F[Unit] =
    F.flatMap(ask.ask)(askCtx => logger.error(askCtx)(message))

  override def warn(message: => String): F[Unit] =
    F.flatMap(ask.ask)(askCtx => logger.warn(askCtx)(message))

  override def info(message: => String): F[Unit] =
    F.flatMap(ask.ask)(askCtx => logger.info(askCtx)(message))

  override def debug(message: => String): F[Unit] =
    F.flatMap(ask.ask)(askCtx => logger.debug(askCtx)(message))

  override def trace(message: => String): F[Unit] =
    F.flatMap(ask.ask)(askCtx => logger.trace(askCtx)(message))

  override def error(t: Throwable)(message: => String): F[Unit] =
    F.flatMap(ask.ask)(askCtx => logger.error(askCtx, t)(message))

  override def warn(t: Throwable)(message: => String): F[Unit] =
    F.flatMap(ask.ask)(askCtx => logger.warn(askCtx, t)(message))

  override def info(t: Throwable)(message: => String): F[Unit] =
    F.flatMap(ask.ask)(askCtx => logger.info(askCtx, t)(message))

  override def debug(t: Throwable)(message: => String): F[Unit] =
    F.flatMap(ask.ask)(askCtx => logger.debug(askCtx, t)(message))

  override def trace(t: Throwable)(message: => String): F[Unit] =
    F.flatMap(ask.ask)(askCtx => logger.trace(askCtx, t)(message))

  override def trace(ctx: Map[String, String])(msg: => String): F[Unit] =
    F.flatMap(ask.ask)(askCtx => logger.trace(askCtx ++ ctx)(msg))

  override def trace(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
    F.flatMap(ask.ask)(askCtx => logger.trace(askCtx ++ ctx, t)(msg))

  override def debug(ctx: Map[String, String])(msg: => String): F[Unit] =
    F.flatMap(ask.ask)(askCtx => logger.debug(askCtx ++ ctx)(msg))

  override def debug(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
    F.flatMap(ask.ask)(askCtx => logger.debug(askCtx ++ ctx, t)(msg))

  override def info(ctx: Map[String, String])(msg: => String): F[Unit] =
    F.flatMap(ask.ask)(askCtx => logger.info(askCtx ++ ctx)(msg))

  override def info(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
    F.flatMap(ask.ask)(askCtx => logger.info(askCtx ++ ctx, t)(msg))

  override def warn(ctx: Map[String, String])(msg: => String): F[Unit] =
    F.flatMap(ask.ask)(askCtx => logger.warn(askCtx ++ ctx)(msg))

  override def warn(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
    F.flatMap(ask.ask)(askCtx => logger.warn(askCtx ++ ctx, t)(msg))

  override def error(ctx: Map[String, String])(msg: => String): F[Unit] =
    F.flatMap(ask.ask)(askCtx => logger.error(askCtx ++ ctx)(msg))

  override def error(ctx: Map[String, String], t: Throwable)(msg: => String): F[Unit] =
    F.flatMap(ask.ask)(askCtx => logger.error(askCtx ++ ctx, t)(msg))

}
