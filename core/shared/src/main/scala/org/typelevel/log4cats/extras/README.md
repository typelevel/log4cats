Important caveats about the loggers in `org.typelevel.log4cats.extras`
======================================================================

`Writer*Logger`
---------------

The loggers provided here backed by `Writer` and `WriterT` come with some important caveats that 
you should be aware of before using.

### General Notes

> **Note**
> These loggers tie their logs to the lifecycle of the return value, so they're generally only useful
when the logs have a similar lifecycle. 
 
> **Warning**
> These loggers should not be used in situations where the logs would be needed if an error occurs (including timeouts).

Basically, they're a way to use `Writer` or `WriterT` with the `log4cats` API. No additional guarantees
are provided. Annotating the happy path is one example of a good use-case for these loggers.

Better alternatives are provided by the `testing` module:
- If a `SelfAwareLogger` is needed for test code, consider
`org.typelevel.log4cats.testing.TestingLogger` over `WriterLogger`

- If a `SelfAwareStructuredLogger` is needed for test code, consider
`org.typelevel.log4cats.testing.StructuredTestingLogger` over `WriterStructuredLogger`

### `WriterLogger` / `WriterStructureLogger`

> **Warning**
> Expect to lose logs if an exception occurs

These are built using `Writer`, which does not directly interact with effects, so expect to do a
non-trivial amount of plumbing if you're planning on using them. Otherwise, if the logs don't matter
in the presence of errors in the context you're using them, they're fine.

### `WriterTLogger` / `WriterTStructuredLogger`

These are built using `WriterT`, and are much easier to use with effects. Running the `WriterT` 
instance will yield a value of type `F[(G[LogMessage], A)]`. 

> **Warning**
> Logged messages can be materialized if *and only if* `F succeeds`

Unfortunately, because of the way that cancellation (and thus timeouts) is handled by 
`cats.effect.IO`, in practice `WriterT` isn't a great fit for anything which can timeout.

`Deferred*Logger` and `Deferred*LoggerFactory`
----------------------------------------------

The deferred `Logger` and `LoggerFactory` subclasses store their log messages in an unbounded 
buffer. This is on the assumption that, if something is being logged, it matters enough to keep
around.

This carries the risk of potentially unbounded memory usage. For example, in situations where 
logging is done by code that is caught in an infinite loop.

If this is a concern, using `DeferredSelfAwareStructuredLogger` or `DeferredLoggerFactory` are recommended, 
as they can reduce the needed buffer space by discarding logs at disabled levels.