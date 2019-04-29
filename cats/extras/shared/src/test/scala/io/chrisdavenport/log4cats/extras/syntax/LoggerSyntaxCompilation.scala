package io.chrisdavenport.log4cats.extras.syntax

import cats._
import io.chrisdavenport.log4cats._
import io.chrisdavenport.log4cats.extras.implicits._

object LoggerSyntaxCompilation {

  def errorLoggerSyntaxMapK[F[_], G[_]](l: ErrorLogger[F])(f: F ~> G) =
    l.mapK(f)

  def messageLoggerSyntaxMapK[F[_], G[_]](l: MessageLogger[F])(f: F ~> G) =
    l.mapK(f)

  def loggerSyntaxMapK[F[_], G[_]](l: Logger[F])(f: F ~> G) =
    l.mapK(f)

  def selfAwareLoggerSyntaxMapK[F[_], G[_]](l: SelfAwareLogger[F])(f: F ~> G) =
    l.mapK(f)

  def structuredLoggerMapK[F[_], G[_]](l: StructuredLogger[F])(f: F ~> G) =
    l.mapK(f)

  def selfAwareStructuredLoggerMapK[F[_], G[_]](l: SelfAwareStructuredLogger[F])(f: F ~> G) =
    l.mapK(f)

}
