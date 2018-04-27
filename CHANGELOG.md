# changelog

This file summarizes **notable** changes for each release, but does not describe internal changes unless they are particularly exciting.

----

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