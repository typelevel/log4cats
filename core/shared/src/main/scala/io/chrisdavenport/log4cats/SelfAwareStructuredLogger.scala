package io.chrisdavenport.log4cats

trait SelfAwareStructuredLogger[F[_]] extends SelfAwareLogger[F] with StructuredLogger[F]

object SelfAwareStructuredLogger {
  def apply[F[_]](implicit ev: SelfAwareStructuredLogger[F]): SelfAwareStructuredLogger[F] = ev

  def withContext[F[_]](sl: SelfAwareStructuredLogger[F])(ctx: Map[String, String]): SelfAwareStructuredLogger[F] =
    new ExtraContextSelfAwareStructuredLogger[F](sl)(ctx)

  private class ExtraContextSelfAwareStructuredLogger[F[_]](sl: SelfAwareStructuredLogger[F])(ctx: Map[String, String])
    extends SelfAwareStructuredLogger[F]{
      private val outer = ctx
      def error(message: => String): F[Unit] = sl.error(outer)(message)
      def warn(message: => String): F[Unit] = sl.warn(outer)(message)
      def info(message: => String): F[Unit] = sl.info(outer)(message)
      def debug(message: => String): F[Unit] = sl.debug(outer)(message)
      def trace(message: => String): F[Unit] = sl.trace(outer)(message)
      def trace(ctx: Map[String,String])(msg: => String): F[Unit] =
        sl.trace(outer ++ ctx)(msg)
      def debug(ctx: Map[String, String])(msg: => String): F[Unit] = 
        sl.debug(outer ++ ctx)(msg)
      def info(ctx: Map[String, String])(msg: => String): F[Unit] = 
        sl.info(outer ++ ctx)(msg)
      def warn(ctx: Map[String, String])(msg: => String): F[Unit] = 
        sl.warn(outer ++ ctx)(msg)
      def error(ctx: Map[String, String])(msg: => String): F[Unit] =
        sl.error(outer ++ ctx)(msg)

      def isTraceEnabled: F[Boolean] = sl.isTraceEnabled
      def isDebugEnabled: F[Boolean] = sl.isDebugEnabled
      def isInfoEnabled: F[Boolean] = sl.isInfoEnabled
      def isWarnEnabled: F[Boolean] = sl.isWarnEnabled
      def isErrorEnabled: F[Boolean] = sl.isErrorEnabled

      /** 
       * Context Logging Is not available on throwable methods 
       */
      def error(t: Throwable)(message: => String): F[Unit] = 
        sl.error(t)(message)
      def warn(t: Throwable)(message: => String): F[Unit] = 
        sl.warn(t)(message)
      def info(t: Throwable)(message: => String): F[Unit] = 
        sl.info(t)(message)
      def debug(t: Throwable)(message: => String): F[Unit] = 
        sl.debug(t)(message)
      def trace(t: Throwable)(message: => String): F[Unit] =
        sl.trace(t)(message)
    }
}