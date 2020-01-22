package io.chrisdavenport.log4cats.slf4j

object Hygiene {
  val scala, Any, String, Unit, classOf = ()
  trait scala; trait Any; trait String; trait Unit
}
