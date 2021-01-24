# changelog

This file summarizes **notable** changes for each release, but does not describe internal changes unless they are particularly exciting.

----

# <a name="2.0.0-M1"></a>New and Noteworthy for Version 2.0.0-M1

- Depends on cats-effect-3.0.0-M5

# <a name="1.2.0-RC2"></a>New and Noteworthy for Version 1.2.0-RC1

- Now published under `org.typelevel`
- Root package changed to `org.typelevel.log4cats`
- Add implicits for `EitherT` and `OptionT` transformers
- Depends on cats-2.3.1 and cats-effect-2.3.1
- Publish for ScalaJS-1.4.0
- Add Dotty crossbuilds for Scala 3.0.0-M2 and 3.0.0-M3

# <a name="1.0.0-RC1"></a>New and Noteworthy for Version 1.0.0-RC1

Many thanks to **Loránd Szakács**, **mr-git**, and **Taylor Brown** for their contributions to this release.

- Introduced Cats Dependency to Core
- Moved Extras into Core
- Simplified Slf4j into 1 pass
- Fixed Bug in Finalizer Based Slf4j Logging
- Dropped 2.11 Support
- Dropped Scalaz (Check PR's for old code you can use)

Dependencies:

- cats 2.0.0-RC1
- In Backends: cats-effect 2.0.0-RC1

# <a name="0.2.0"></a>New and Noteworthy for Version 0.2.0

Many thanks to **λoλcat** and **Loránd Szakács** for their contributions to this release.

Primary changes in the new release are additional features and version upgrades.
Significantly, the upgrade to cats-effect 1.0 and log4scalaz being transitioned onto
the ZIO project.

New features:

- New Extras Project which contains
  - Writer Based Logger
  - MapK transformation Functions
  - Implicit Syntax Enhancements adding mapK directly onto the algebra.

Bug Fixes:

- Macro Failed to Capture Enclosing Class on safe Slf4jLogger construction. This has been remedied.

# <a name="0.2.0-RC2"></a>New and Noteworthy for Version 0.2.0-RC2

This is expected to be the last release prior to the 0.2.0 release.

- Loggers for Writer added to cats-extras for pure logging without effects.
- Upgrades
  - specs2 4.3.5

# <a name="0.2.0-RC1"></a>New and Noteworthy for Version 0.2.0-RC1

- Added cats-extras, which exposes functions for Rank2 functor behavior for all of the algebras.
- Upgrades
  - scribe

# <a name="0.2.0-M1"></a>New and Noteworthy for Version 0.2.0-M1

Transitioned Scalaz ZIO from scalaz-ioeffect to scalaz-zio. 

- Upgrades
  - cats 1.4.0
  - cats-effect 1.0.0 - bincompat breaking
  - scribe 2.6.0 - bincompat breaking
  - specs2
  
## <a name="0.1.1"></a>New and Noteworthy for Version 0.1.1

- Adopt Scala Code of Conduct
- Publish log4cats-noop, a logger implementation that does nothing.
  
## <a name="0.1.0"></a>New and Noteworthy for Version 0.1.0

- Upgrades
  - specs2
  - scribe
  - scalajs
  - tut (docs)
  - github4s (docs)

## <a name="0.0.7"></a>New and Noteworthy for Version 0.0.7

- Add Implicit Summoning for Logger Hierarchy Traits [#43](https://github.com/ChristopherDavenport/log4cats/pull/43)

## <a name="0.0.6"></a>New and Noteworthy for Version 0.0.6

Remove cats dependency from log4cats-core. Publish a log4s module with scalaz-ioeffect module name log4scalaz-log4s.

## <a name="0.0.5"></a>New and Noteworthy for Version 0.0.5

Transition to a hierarchical logger algebra, which has logger as central and capabilities such as self-awareness of logging capabilities, and structured logging as additional abilitites. slf4j wrapper specifically optimized for log4cats, testing logger improvements, apache licensing transition

- Hierarchical Logging Algebras [#34](https://github.com/ChristopherDavenport/log4cats/pull/34)
- slf4j-api optimized logger implementation [#33](https://github.com/ChristopherDavenport/log4cats/pull/33)
- Upgrades
  - scribe 2.4.0

Thanks to Loránd Szakács and Joe Kachmar in helping get this release ready.

## <a name="0.0.4"></a>New and Noteworthy for Version 0.0.4

ScalaJS Support

- Compiled primary algebra to ScalaJS
- Built Scribe backend with ScalaJS as well
- Testing project added which implements a logger with ability to check logs

## <a name="0.0.3"></a>New and Noteworthy for Version 0.0.3

Multi Project Support Migration

- Added Experimental Support for Scribe
- Log4s now has a dedication Log4sLogger for build functions

## <a name="0.0.2"></a>New and Noteworthy for Version 0.0.2

Minor release addressing initial concerns.

- Made calls to algebra lazy so when disabled are not evaluated. [#4](https://github.com/ChristopherDavenport/log4cats/pull/4)
- Access to whether a particular log level is enabled. [#5](https://github.com/ChristopherDavenport/log4cats/pull/5)
- withModifiedString for modifying the logged message. [#6](https://github.com/ChristopherDavenport/log4cats/pull/6)

## <a name="0.0.1"></a>New and Noteworthy for Version 0.0.1

Initial Release

- Initial Algebra Release
- Initial Log4s Support
