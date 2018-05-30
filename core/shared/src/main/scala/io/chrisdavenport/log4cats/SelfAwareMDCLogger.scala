package io.chrisdavenport.log4cats

trait SelfAwareMDCLogger[F[_]] extends SelfAwareLogger[F] with MDCLogger[F]