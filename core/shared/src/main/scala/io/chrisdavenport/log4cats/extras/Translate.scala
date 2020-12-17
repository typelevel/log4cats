package io.chrisdavenport.log4cats.extras

import io.chrisdavenport.log4cats._
import cats._

/**
 * Allows for mapK or algebraic higher kinded
 * transformations
 */
object Translate {

  @deprecated("0.4.0", "Use mapK directly on the ErrorLogger")
  def errorLogger[G[_], F[_]](f: G ~> F)(logger: ErrorLogger[G]): ErrorLogger[F] =
    logger.mapK(f)

  @deprecated("0.4.0", "Use mapK directly on the Logger")
  def logger[G[_], F[_]](f: G ~> F)(logger: Logger[G]): Logger[F] =
    logger.mapK(f)

  @deprecated("0.4.0", "Use mapK directly on the MessageLogger")
  def messageLogger[G[_], F[_]](f: G ~> F)(logger: MessageLogger[G]): MessageLogger[F] =
    logger.mapK(f)

  @deprecated("0.4.0", "Use mapK directly on the SelfAwareLogger")
  def selfAwareLogger[G[_], F[_]](f: G ~> F)(logger: SelfAwareLogger[G]): SelfAwareLogger[F] =
    logger.mapK(f)

  @deprecated("0.4.0", "Use mapK directly on the SelfAwareStructuredLogger")
  def selfAwareStructuredLogger[G[_], F[_]](
      f: G ~> F
  )(logger: SelfAwareStructuredLogger[G]): SelfAwareStructuredLogger[F] =
    logger.mapK(f)

  @deprecated("0.4.0", "Use mapK directly on the StructuredLogger")
  def structuredLogger[G[_], F[_]](f: G ~> F)(logger: StructuredLogger[G]): StructuredLogger[F] =
    logger.mapK(f)

}
