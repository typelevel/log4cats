package io.chrisdavenport.log4cats

import org.specs2._

object MainSpec extends mutable.Specification {

  "Main" should {
    "run a println" in {
      Main.main(Array.empty[String]).should_===(())
    }
  }

}