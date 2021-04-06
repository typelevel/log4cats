/*
 * Copyright 2018 Christopher Davenport
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.typelevel.log4cats

// TODO: figure out why the implicitNotFound annotation does not work on Scala 3.
class LoggingImplicitNotFoundTests extends munit.FunSuite {

  test("receive proper implicitNotFound error message on Logging type") {
    val errors = compileErrors(
      "import cats.effect.IO;import org.typelevel.log4cats.Logging;Logging[IO].create"
    )
    assert(
      errors.contains(
        "you do actually have a GenLogger[Id, L], but where L is not SelfAwareStructuredLogger"
      ),
      clue = s"""|Actual compiler error was:
                 |
                 |$errors
                 |
                 |
                 |""".stripMargin
    )
  }

}
