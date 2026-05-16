# Changelog for Minecraft 1.20.1
All notable changes to this project will be documented in this file.

<a name="1.20.1-1.0.17"></a>
## [1.20.1-1.0.17](/compare/1.20.1-1.0.16...1.20.1-1.0.17) - 2026-05-16 10:17:12


### Fixed
* Fix external script edits not syncing when directory is created before disk (#62), Closes #61

<a name="1.20.1-1.0.16"></a>
## [1.20.1-1.0.16](/compare/1.20.1-1.0.15...1.20.1-1.0.16) - 2025-12-22 15:42:05 +0100


### Fixed
* Tweak relocation of GraalVM to fix incompatibilities
  This fixes incompatibilities with Cobblemon and JsMacros.
  This also includes an update to Graal 25.0.1.
  Closes #34
  Closes #53
  Closes #39
* Fix crash after pasting text that reduces text area size, Closes #52

<a name="1.20.1-1.0.15"></a>
## [1.20.1-1.0.15](/compare/1.20.1-1.0.14...1.20.1-1.0.15) - 2025-10-07 07:38:06 +0200


### Added
* Add ValueTranslatorRegisterEvent for mod compats (required for Integrated Mekanism)

<a name="1.20.1-1.0.14"></a>
## [1.20.1-1.0.14](/compare/1.20.1-1.0.13...1.20.1-1.0.14) - 2025-06-01 10:58:00 +0200


### Fixed
* Fix some modded keybindings activating in scripting terminal, Closes #40

<a name="1.20.1-1.0.13"></a>
## [1.20.1-1.0.13](/compare/1.20.1-1.0.12...1.20.1-1.0.13) - 2025-02-21 16:03:56 +0100


### Changed
* Restrict access to parts of the Java API
  They can still be enabled if desired by the server admin.

<a name="1.20.1-1.0.12"></a>
## [1.20.1-1.0.12](/compare/1.20.1-1.0.11...1.20.1-1.0.12) - 2025-02-08 17:38:55 +0100


### Fixed
* Use jar-in-jar instead of shadowing, Closes #34

<a name="1.20.1-1.0.11"></a>
## [1.20.1-1.0.11](/compare/1.20.1-1.0.10...1.20.1-1.0.11) - 2025-01-17 16:51:13 +0100


### Fixed
* Re-enable shadow renames, except for com.oracle.truffle, Closes #32
  This makes this mod work correctle on all JVMs, including GraalVM 21 and 23.

<a name="1.20.1-1.0.10"></a>
## [1.20.1-1.0.10](/compare/1.20.1-1.0.9...1.20.1-1.0.10) - 2025-01-11 10:11:06 +0100


### Fixed
* Update to graal 24.1.1, fixes issues for some JDK 23 users, Closes #30

<a name="1.20.1-1.0.9"></a>
## [1.20.1-1.0.9](/compare/1.20.1-1.0.8...1.20.1-1.0.9) - 2024-12-28 15:31:32 +0100


### Fixed
* Fix infinite loop not being properly caught, Closes #28

<a name="1.20.1-1.0.8"></a>
## [1.20.1-1.0.8](/compare/1.20.1-1.0.7...1.20.1-1.0.8) - 2024-12-23 13:59:08 +0100


### Fixed
* Fix display panel crash when translation undefined values, Closes #27

<a name="1.20.1-1.0.7"></a>
## [1.20.1-1.0.7](/compare/1.20.1-1.0.6...1.20.1-1.0.7) - 2024-12-17 11:08:27 +0100


### Changed
* Cache scripts to improve performance
  Required for CyclopsMC/IntegratedDynamics#1439

### Fixed
* Fix variable card writing failing when reselecting text, Closes #25

<a name="1.20.1-1.0.6"></a>
## [1.20.1-1.0.6](/compare/1.20.1-1.0.5...1.20.1-1.0.6) - 2024-12-16 10:58:28 +0100


### Fixed
* Fix null object values not being skipped to NBT, Closes #22
* Fix delete not being highlighted as keyword, Closes #23
* Fix NPE in ScriptingData if FileWatcher was not initialized

<a name="1.20.1-1.0.5"></a>
## [1.20.1-1.0.5](/compare/1.20.1-1.0.4...1.20.1-1.0.5) - 2024-10-22 16:23:33 +0200


### Fixed
* Fix NPE in ScriptingData if FileWatcher was not initialized

<a name="1.20.1-1.0.4"></a>
## [1.20.1-1.0.4](/compare/1.20.1-1.0.3...1.20.1-1.0.4) - 2024-07-20 15:49:58 +0200


### Fixed
* Fix config file not being generated, Closes #14

<a name="1.20.1-1.0.3"></a>
## [1.20.1-1.0.3](/compare/1.20.1-1.0.2...1.20.1-1.0.3) - 2024-06-23 07:11:02 +0200


### Fixed
* Fix incorrect error message for missing member
* Fix invalid script member error not being caught, Closes #10
* Fix stack size example errors in book, Closes #13

<a name="1.20.1-1.0.2"></a>
## [1.20.1-1.0.2](/compare/1.20.1-1.0.1...1.20.1-1.0.2) - 2024-04-14 14:18:28 +0200


### Changed
* Add script context to errors during translation

### Fixed
* Automatically invalidate network errors on script changes, Closes #5
* Don't hard-crash on unsupported conversions to NBT, Closes #8
* Fix missing type checking for script variables in aspects, Closes #9

<a name="1.20.1-1.0.1"></a>
## [1.20.1-1.0.1](/compare/1.20.1-1.0.0...1.20.1-1.0.1) - 2024-02-06 19:07:07 +0100


### Fixed
* Fix crash when clicking in terminal without selected disk

<a name="1.20.1-1.0.0"></a>
## [1.20.1-1.0.0] - 2024-02-04 16:12:10 +0100


Initial release
