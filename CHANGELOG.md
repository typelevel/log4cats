# changelog

This file summarizes **notable** changes for each release, but does not describe internal changes unless they are particularly exciting.

----

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