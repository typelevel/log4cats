package io.chrisdavenport.log4cats.extras.syntax

import cats.~>
import io.chrisdavenport.log4cats._
import io.chrisdavenport.log4cats.extras.Translate

private[extras] trait loggersyntax {
  implicit class messageLoggerSyntax[F[_]](m: MessageLogger[F]){
    def mapK[G[_]](f: F ~> G): MessageLogger[G] = Translate.messageLogger(f)(m)
  }
  implicit class errorLoggerSyntax[F[_]](m: ErrorLogger[F]){
    def mapK[G[_]](f: F ~> G): ErrorLogger[G] = Translate.errorLogger(f)(m)
  }
  implicit class loggerSyntax[F[_]](m: Logger[F]){
    def mapK[G[_]](f: F ~> G): Logger[G] = Translate.logger(f)(m)
  }
  implicit class selfAwareLoggerSyntax[F[_]](m: SelfAwareLogger[F]){
    def mapK[G[_]](f: F ~> G): SelfAwareLogger[G] = Translate.selfAwareLogger(f)(m)
  }
  implicit class structuredLoggerSyntax[F[_]](m: StructuredLogger[F]){
    def mapK[G[_]](f: F ~> G): StructuredLogger[G] = Translate.structuredLogger(f)(m)
  }
  implicit class selfAwareStructuredLogger[F[_]](m: SelfAwareStructuredLogger[F]){
    def mapK[G[_]](f: F ~> G): SelfAwareStructuredLogger[G] =
      Translate.selfAwareStructuredLogger(f)(m)
  }
}