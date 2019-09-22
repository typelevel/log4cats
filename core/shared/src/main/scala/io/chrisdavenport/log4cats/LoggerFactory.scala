package io.chrisdavenport.log4cats

trait LoggerFactory[F[_]] {

  def fromName(name: String): F[SelfAwareStructuredLogger[F]]

  def fromClass(clazz: Class[_]): F[SelfAwareStructuredLogger[F]]

}
