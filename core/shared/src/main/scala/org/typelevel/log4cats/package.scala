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

package org.typelevel

package object log4cats {

  /**
   * Convenience type for describing Logging where creation of the loggers itself is a pure operation
   *
   * @tparam F[_]
   *  the effect type in which logging is done.
   */
  @scala.annotation.implicitNotFound(
    "Not found for Logging[${F}], keep in mind that ${F} is the effect in which logging is done. e.g. `Logging[${F}].create.info(message) : ${F}[Unit]`.\nAdditionally Logging[${F}] is defined as creating only loggers of type SelfAwareStructuredLogger[${F}].\nThe Logging[${F}] type itself described logging creation to be pure, i.e. cats.Id.\nSo your problem might be:\n\t1) you only have a GenLogger[G[_], ...] for some G[_] type other than cats.Id\n\t2) you do actually have a GenLogger[Id, L], but where L is not SelfAwareStructuredLogger[${F}].\nIf you are unsure how to create a Logging[${F}], then you can to look at the `log4cats-slf4j` module, or `log4cats-noop` for concrete implementations. Example for slf4j:\nimplicit val logging: Logging[IO] = Slf4jLogging.forSync[IO] // we create our Logging[IO]\nLogging[IO].create //we summon our instance, and create a SelfAwareStructuredLogger[IO]."
  )
  type Logging[F[_]] = GenLogging[cats.Id, SelfAwareStructuredLogger[F]]

  /**
   * Convenience type
   *
   * @tparam F[_]
   *  the effect type of both logger creation, and logging.
   */
  @scala.annotation.implicitNotFound(
    "Not found for LoggingF[${F}], keep in mind that ${F} is the effect in which logging is done, and the effect in which loggers are created. e.g. `LoggingF[${F}].create.flatMap(_.info(message)) : ${F}[Unit]`.\nAdditionally LoggingF[${F}] is defined as creating only loggers of type SelfAwareStructuredLogger[${F}].\nSo your problem might be:\n\t1) you only have a GenLogger[G[_], ...] for some G[_] type other than ${F}\n\t2) you do actually have a GenLogger[${F}, L], but where L is not SelfAwareStructuredLogger[${F}].\nIf you are unsure how to create a LoggingF[${F}], then you can to look at the `log4cats-slf4j` module for concrete implementations. Example for slf4j:\nimplicit val logging: LoggingF[IO] = Slf4jGenLogging.forSync[IO, IO] // we create our Logging[IO]\nLogging[IO].create //we summon our instance, and create an IO[SelfAwareStructuredLogger[IO]]."
  )
  type LoggingF[F[_]] = GenLogging[F, SelfAwareStructuredLogger[F]]

}
