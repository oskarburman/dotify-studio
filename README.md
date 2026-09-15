[![Type](https://img.shields.io/badge/type-application-blue.svg)](https://github.com/brailleapps/wiki/wiki/Types)
[![License: LGPL v2.1](https://img.shields.io/badge/License-LGPL%20v2%2E1%20%28or%20later%29-blue.svg)](https://www.gnu.org/licenses/lgpl-2.1)

# Dotify Studio #
Provides an accessible graphical user interface for creating, managing and embossing PEF-files.

This is a fork of [brailleapps/dotify-studio](https://github.com/brailleapps/dotify-studio), which is no longer maintained. Changes in this fork:
 - Builds and runs on Java 17+ with OpenJFX
 - Default page height is 28 rows and the cover page is off by default (both can still be changed in the converter options)
 - Fixed: a typed number of copies was ignored unless Enter was pressed
 - Pauses between copies when embossing several copies, showing how many copies remain (Enter continues, Escape cancels the remaining copies)

## Using ##
Download the latest release and install (or unpack) it. For more information, see the user guide in the docs folder.

## Building ##
Build with `gradlew installDist` (Windows) or `./gradlew installDist` (Mac/Linux), using JDK 17. Start the application with `build/install/dotify-studio/bin/dotify-studio`.

Note that the JavaFX libraries are platform specific, so the distribution must be built on the platform where it will run.

## Installers ##
To build installers, see [dotify-studio-installer](https://github.com/brailleapps/dotify-studio-installer).

## Requirements & Compatibility ##
 - Requires Java 17 or later

## More information ##
See the [common wiki](https://github.com/brailleapps/wiki/wiki) for more information.