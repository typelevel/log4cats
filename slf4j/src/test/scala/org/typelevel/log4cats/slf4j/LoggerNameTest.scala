package org.typelevel.log4cats.slf4j

import munit.FunSuite

class LoggerNameTest extends FunSuite {
  class FooA {
    val name = LoggerName.name
  }

  object FooB {
    val name = LoggerName.name
  }

  test("names") {
    val name1 = new FooA().name
    val name2 = FooB.name

    assertEquals(name1.value, "org.typelevel.log4cats.slf4j.LoggerNameTests.FooA")
    assertEquals(name2.value, "org.typelevel.log4cats.slf4j.LoggerNameTests.FooB")
  }
}
