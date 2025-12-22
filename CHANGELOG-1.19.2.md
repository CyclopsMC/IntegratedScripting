# Changelog for Minecraft 1.19.2
All notable changes to this project will be documented in this file.

<a name="1.19.2-1.0.12"></a>
## [1.19.2-1.0.12](/compare/1.19.2-1.0.11...1.19.2-1.0.12) - 2025-12-22 15:37:47


### Fixed
* Fix crash after pasting text that reduces text area size, Closes #52

<a name="1.19.2-1.0.11"></a>
## [1.19.2-1.0.11](/compare/1.19.2-1.0.10...1.19.2-1.0.11) - 2025-06-01 11:01:06 +0200


### Fixed
* Fix some modded keybindings activating in scripting terminal, Closes #40

<a name="1.19.2-1.0.10"></a>
## [1.19.2-1.0.10](/compare/1.19.2-1.0.9...1.19.2-1.0.10) - 2025-02-21 16:02:12 +0100


### Changed
* Restrict access to parts of the Java API
  They can still be enabled if desired by the server admin.

<a name="1.19.2-1.0.9"></a>
## [1.19.2-1.0.9](/compare/1.19.2-1.0.8...1.19.2-1.0.9) - 2024-12-28 15:30:37 +0100


### Fixed
* Fix infinite loop not being properly caught, Closes #28

<a name="1.19.2-1.0.8"></a>
## [1.19.2-1.0.8](/compare/1.19.2-1.0.7...1.19.2-1.0.8) - 2024-12-23 13:57:56 +0100


### Fixed
* Fix display panel crash when translation undefined values, Closes #27

<a name="1.19.2-1.0.7"></a>
## [1.19.2-1.0.7](/compare/1.19.2-1.0.6...1.19.2-1.0.7) - 2024-12-17 11:06:57 +0100


### Changed
* Cache scripts to improve performance
  Required for CyclopsMC/IntegratedDynamics#1439

### Fixed
* Fix variable card writing failing when reselecting text, Closes #25

<a name="1.19.2-1.0.6"></a>
## [1.19.2-1.0.6](/compare/1.19.2-1.0.5...1.19.2-1.0.6) - 2024-12-16 10:56:17 +0100


### Fixed
* Fix null object values not being skipped to NBT, Closes #22
* Fix delete not being highlighted as keyword, Closes #23

<a name="1.19.2-1.0.5"></a>
## [1.19.2-1.0.5](/compare/1.19.2-1.0.4...1.19.2-1.0.5) - 2024-10-22 16:22:57 +0200


### Fixed
* Fix NPE in ScriptingData if FileWatcher was not initialized

<a name="1.19.2-1.0.4"></a>
## [1.19.2-1.0.4](/compare/1.19.2-1.0.3...1.19.2-1.0.4) - 2024-07-20 15:48:11 +0200


### Fixed
* Fix config file not being generated, Closes #14

<a name="1.19.2-1.0.3"></a>
## [1.19.2-1.0.3](/compare/1.19.2-1.0.2...1.19.2-1.0.3) - 2024-06-23 07:08:47 +0200


### Fixed
* Fix incorrect error message for missing member
* Fix invalid script member error not being caught, Closes #10
* Fix stack size example errors in book, Closes #13

<a name="1.19.2-1.0.2"></a>
## [1.19.2-1.0.2](/compare/1.19.2-1.0.1...1.19.2-1.0.2) - 2024-04-14 14:15:14 +0200


### Changed
* Add script context to errors during translation

### Fixed
* Automatically invalidate network errors on script changes, Closes #5
* Don't hard-crash on unsupported conversions to NBT, Closes #8
* Fix missing type checking for script variables in aspects, Closes #9

<a name="1.19.2-1.0.1"></a>
## [1.19.2-1.0.1](/compare/1.19.2-1.0.0...1.19.2-1.0.1) - 2024-02-06 19:05:07 +0100


### Fixed
* Fix crash when clicking in terminal without selected disk

<a name="1.19.2-1.0.0"></a>
## [1.19.2-1.0.0] - 2024-02-04 16:10:48 +0100


Initial release
