# Changelog for Minecraft 1.21.1
All notable changes to this project will be documented in this file.

<a name="1.21.1-1.0.25"></a>
## [1.21.1-1.0.25](https://github.com/CyclopsMC/IntegratedScripting/compare/1.21.1-1.0.24...1.21.1-1.0.25) - 2026-08-04 07:40:56


### Changed
* Update to Graal 25.1.3

### Fixed
* Fix unable to run on some OSes such as OpenBSD
* Don't crash when evaluating a non-existing script

<a name="1.21.1-1.0.24"></a>
## [1.21.1-1.0.24](https://github.com/CyclopsMC/IntegratedScripting/compare/1.21.1-1.0.23...1.21.1-1.0.24) - 2026-07-22 16:26:07 +0200


### Fixed
* Rewrite shadowing graal using fallback classloader
This commit has been cherry-picked from @wagyourtail's PR in #36.
Closes #63

<a name="1.21.1-1.0.23"></a>
## [1.21.1-1.0.23](https://github.com/CyclopsMC/IntegratedScripting/compare/1.21.1-1.0.22...1.21.1-1.0.23) - 2026-07-13 20:13:16 +0200


### Fixed
* Check for path safety when setting scripts

<a name="1.21.1-1.0.22"></a>
## [1.21.1-1.0.22](https://github.com/CyclopsMC/IntegratedScripting/compare/1.21.1-1.0.21...1.21.1-1.0.22) - 2026-05-16 10:18:59 +0200


### Fixed
* Fix external script edits not syncing when directory is created before disk (#62), Closes #61

<a name="1.21.1-1.0.21"></a>
## [1.21.1-1.0.21](https://github.com/CyclopsMC/IntegratedScripting/compare/1.21.1-1.0.20...1.21.1-1.0.21) - 2025-12-24 13:43:13 +0100


### Fixed
* Fix Graal not being bumped to 25.0.1, Closes #54

<a name="1.21.1-1.0.20"></a>
## [1.21.1-1.0.20](https://github.com/CyclopsMC/IntegratedScripting/compare/1.21.1-1.0.19...1.21.1-1.0.20) - 2025-12-22 15:54:19 +0100


### Added
* Add translations through Crowdin (#50)

### Fixed
* Tweak relocation of GraalVM to fix incompatibilities
  This fixes incompatibilities with Cobblemon and JsMacros.
  This also includes an update to Graal 25.0.1.
  Closes #34
  Closes #53
  Closes #39
* Fix crash after pasting text that reduces text area size, Closes #52

<a name="1.21.1-1.0.19"></a>
## [1.21.1-1.0.19](https://github.com/CyclopsMC/IntegratedScripting/compare/1.21.1-1.0.18...1.21.1-1.0.19) - 2025-10-07 07:50:49 +0200


### Added
* Add ValueTranslatorRegisterEvent for mod compats (required for Integrated Mekanism)
* Add translations through Crowdin (#29)
* Add PT_BR localization (#48)

### Fixed
* Fix some spelling and grammar typos in lang (#47)
* Fix spelling and grammar typos in lang (#45)

<a name="1.21.1-1.0.18"></a>
## [1.21.1-1.0.18](https://github.com/CyclopsMC/IntegratedScripting/compare/1.21.1-1.0.17...1.21.1-1.0.18) - 2025-06-01 10:59:07 +0200


### Fixed
* Fix some modded keybindings activating in scripting terminal, Closes #40

<a name="1.21.1-1.0.17"></a>
## [1.21.1-1.0.17](https://github.com/CyclopsMC/IntegratedScripting/compare/1.21.1-1.0.16...1.21.1-1.0.17) - 2025-02-21 16:08:49 +0100


### Changed
* Restrict access to parts of the Java API
  They can still be enabled if desired by the server admin.

<a name="1.21.1-1.0.16"></a>
## [1.21.1-1.0.16](https://github.com/CyclopsMC/IntegratedScripting/compare/1.21.1-1.0.15...1.21.1-1.0.16) - 2025-02-15 10:21:10 +0100


### Fixed
* Fix broken advancement icons

<a name="1.21.1-1.0.15"></a>
## [1.21.1-1.0.15](https://github.com/CyclopsMC/IntegratedScripting/compare/1.21.1-1.0.14...1.21.1-1.0.15) - 2025-02-08 17:37:34 +0100


### Fixed
* Use jar-in-jar instead of shadowing, Closes #34

<a name="1.21.1-1.0.14"></a>
## [1.21.1-1.0.14](https://github.com/CyclopsMC/IntegratedScripting/compare/1.21.1-1.0.13...1.21.1-1.0.14) - 2025-01-17 16:44:48 +0100


### Fixed
* Re-enable shadow renames, except for com.oracle.truffle, Closes #32
  This makes this mod work correctle on all JVMs, including GraalVM 21 and 23.

<a name="1.21.1-1.0.13"></a>
## [1.21.1-1.0.13](https://github.com/CyclopsMC/IntegratedScripting/compare/1.21.1-1.0.12...1.21.1-1.0.13) - 2025-01-11 10:07:39 +0100


### Fixed
* Stop shadow renaming to fix breakage for graal 24+

<a name="1.21.1-1.0.12"></a>
## [1.21.1-1.0.12](https://github.com/CyclopsMC/IntegratedScripting/compare/1.21.1-1.0.11...1.21.1-1.0.12) - 2025-01-11 09:41:20 +0100


### Fixed
* Update to graal 24.1.1, fixes issues for some JDK 23 users, Closes #30

<a name="1.21.1-1.0.11"></a>
## [1.21.1-1.0.11](https://github.com/CyclopsMC/IntegratedScripting/compare/1.21.1-1.0.10...1.21.1-1.0.11) - 2024-12-28 15:33:05 +0100


### Fixed
* Fix infinite loop not being properly caught, Closes #28

<a name="1.21.1-1.0.10"></a>
## [1.21.1-1.0.10](https://github.com/CyclopsMC/IntegratedScripting/compare/1.21.1-1.0.9...1.21.1-1.0.10) - 2024-12-23 14:00:13 +0100


### Fixed
* Fix display panel crash when translation undefined values, Closes #27

<a name="1.21.1-1.0.9"></a>
## [1.21.1-1.0.9](https://github.com/CyclopsMC/IntegratedScripting/compare/1.21.1-1.0.8...1.21.1-1.0.9) - 2024-12-17 11:09:16 +0100


### Changed
* Cache scripts to improve performance
  Required for CyclopsMC/IntegratedDynamics#1439

### Fixed
* Fix variable card writing failing when reselecting text, Closes #25

<a name="1.21.1-1.0.8"></a>
## [1.21.1-1.0.8](https://github.com/CyclopsMC/IntegratedScripting/compare/1.21.1-1.0.7...1.21.1-1.0.8) - 2024-12-16 11:01:48 +0100


### Fixed
* Fix null object values not being skipped to NBT, Closes #22
* Fix delete not being highlighted as keyword, Closes #23
* Fix NPE in ScriptingData if FileWatcher was not initialized

<a name="1.21.1-1.0.7"></a>
## [1.21.1-1.0.7](https://github.com/CyclopsMC/IntegratedScripting/compare/1.21.1-1.0.6...1.21.1-1.0.7) - 2024-11-22 07:14:05 +0100


### Fixed
* Fix unable to clear part IDs, Closes CyclopsMC/IntegratedTunnels#309

<a name="1.21.1-1.0.6"></a>
## [1.21.1-1.0.6] - 2024-10-22 16:20:24 +0200


### Fixed
* Fix NPE in ScriptingData if FileWatcher was not initialized
