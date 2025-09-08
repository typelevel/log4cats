package logger

/** Typeclass representing the notion that a value can contribute to a log, by
  * transforming it in some way.
  */
trait Recordable[A] {
  def record(value: => A): LogRecord
}

object Recordable {

  def apply[A](implicit ev: Recordable[A]): ev.type = ev

  implicit val stringLoggable: Recordable[String] = new Recordable[String] {
    def record(value: => String) = _.withMessage(value)
  }

  implicit def tupleLoggable[T: Context.Encoder]: Recordable[(String, T)] =
    new Recordable[(String, T)] {

      override def record(value: => (String, T)): LogRecord = {
        val (k, v) = value
        (_: Log.Builder).withContext(k)(v)
      }

    }

  implicit def throwableLoggable[T <: Throwable]: Recordable[T] =
    new Recordable[T] {
      def record(value: => T): LogRecord = _.withThrowable(value)
    }

}
