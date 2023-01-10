package org.typelevel.log4cats.natchez

import cats._
import cats.data.Kleisli
import cats.effect.{Trace => _, _}
import cats.syntax.all._
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.exporter.logging.LoggingSpanExporter
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter
import io.opentelemetry.sdk.resources.{Resource => OTResource}
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.`export`.{BatchSpanProcessor, SimpleSpanProcessor}
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes
import natchez._
import natchez.opentelemetry._
import org.typelevel.log4cats._

import scala.util.control.NoStackTrace

object NatchezExample extends IOApp.Simple {
  private def app[F[_] : Applicative : Trace]: F[Unit] =
    StructuredLogger[F].info(Map("it's me" -> "hi"))("hello") *>
      StructuredLogger[F].warn(Map("hi" -> "I'm the problem it's me"), new RuntimeException("boom") with NoStackTrace {})("Hmm, might be a problem")

  override def run: IO[Unit] =
    OpenTelemetry.entryPoint[IO](globallyRegister = true) { builder =>
      Resource.fromAutoCloseable(IO {
        BatchSpanProcessor.builder(OtlpGrpcSpanExporter.builder().build).build()
      })
        .flatMap { bsp =>
          Resource.fromAutoCloseable(IO {
            SdkTracerProvider
              .builder()
              .setResource {
                OTResource
                  .getDefault
                  .merge(OTResource.create(Attributes.of(
                    ResourceAttributes.SERVICE_NAME, "NatchezExample",
                  )))
              }
              .addSpanProcessor(SimpleSpanProcessor.create(LoggingSpanExporter.create))
              .addSpanProcessor(bsp)
              .build()
          })
        }
        .evalMap(stp => IO(builder.setTracerProvider(stp)))
    }
      .use { entryPoint =>
        entryPoint.root("NatchezExample")
          .use(app[Kleisli[IO, Span[IO], *]].run)
      }
}
