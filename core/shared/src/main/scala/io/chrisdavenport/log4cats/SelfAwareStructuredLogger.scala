package io.chrisdavenport.log4cats

trait SelfAwareStructuredLogger[F[_]] extends SelfAwareLogger[F] with StructuredLogger[F]