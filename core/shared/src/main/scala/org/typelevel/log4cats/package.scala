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
    """
Implicit not found for Logging[${F}]. Which is a convenience type alias.

Keep in mind that:

a) Logging[${F}] has to be created _explicitely_ once (at least) in your code

b) for the type Logging, ${F} is _only_ the effect in which logging is done.
    ```
    Logging[${F}].create.info(message) : ${F}[Unit]
    ```

c) The Logging[${F}] type itself described logging creation as being pure, i.e. in cats.Id

d) Logging[${F}] is defined as creating _only_ loggers of type SelfAwareStructuredLogger[${F}].

The full definition of Logging is:
    ```
    type Logging[F[_]] = GenLogging[cats.Id, SelfAwareStructuredLogger[F]]
    ```

Therefore, your problem might be:
    1) you didn't create a Logging[${F}] to begin with, or didn't mark it as implicit
    2) you only have a GenLogger[G[_], SelfAwareStructuredLogger[${F}]] for some G[_] type other than cats.Id
    3) you do actually have a GenLogger[Id, L], but where L is not SelfAwareStructuredLogger[${F}].

If you are unsure how to create a Logging[${F}], then you can to look at the `log4cats-slf4j`, 
or `log4cats-noop` modules for concrete implementations.

Example for slf4j:
    ```
    import cats.effect.IO

    // we create our Logging[IO]
    implicit val logging: Logging[IO] = Slf4jLogging.forSync[IO]

    //we summon our instance, and create our logger
    val logger: SelfAwareStructuredLogger[IO] = Logging[IO].create
    logger.info("logging in IO!"): IO[Unit]
    ```
"""
  )
  type Logging[F[_]] = GenLogging[cats.Id, SelfAwareStructuredLogger[F]]

  /**
   * Convenience type
   *
   * @tparam F[_]
   *  the effect type of both logger creation, and logging.
   */
  @scala.annotation.implicitNotFound(
    """
Implicit not found for LoggingF[${F}]. Which is a convenience type alias.

Keep in mind that:

a) LoggingF[${F}] has to be created _explicitely_ once (at least) in your code

b) for the type LoggingF, ${F} is _both_ the effect in which logger creation is done
   _and_ the effect in which logging is done
    ```
    val loggerF: ${F}[SelfAwareStructuredLogger] = Logging[${F}].create
    loggerF.flatMap{logger => logger.info("the result of the .info is in F"): ${F}[Unit] }: ${F}[Unit]
    ```

c) LoggingF[${F}] is defined as creating _only_ loggers of type SelfAwareStructuredLogger[${F}].

The full definition of Logging is:
    ```
    type LoggingF[F[_]] = GenLogging[F, SelfAwareStructuredLogger[F]]
    ```
Therefore, your problem might be:
    1) you didn't create a LoggingF[${F}] to begin with, or didn't mark it as implicit
    2) you only have a GenLoggerF[G[_], SelfAwareStructuredLogger[${F}]] for some G[_] type other than type ${F}
    3) you do actually have a GenLogger[${F}, L], but where L is not SelfAwareStructuredLogger[${F}].

If you are unsure how to create a LoggingF[${F}], then you can to look at the 
`log4cats-slf4j` module for concrete implementations.

Example for slf4j:
    ```
    import cats.effect.IO

    // we use IO for both logger creation effect, and logging effect. Otherwise it wouldn't be a LoggingF
    implicit val loggingF: LoggingF[IO] = Slf4jGenLogging.forSync[IO, IO]

    //we summon our instance, and create our logger
    val loggerIO: IO[SelfAwareStructuredLogger[IO]] = LoggingF[IO].create

    loggerIO.flatMap{logger => logger.info("logging in IO"): IO[Unit]}: IO[Unit]
    ```
"""
  )
  type LoggingF[F[_]] = GenLogging[F, SelfAwareStructuredLogger[F]]

}
