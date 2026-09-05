# Changelog for Minecraft 26.1.2
All notable changes to this project will be documented in this file.

<a name="26.1.2-1.0.28"></a>
## [26.1.2-1.0.28](https://github.com/CyclopsMC/IntegratedScripting/compare/26.1.2-1.0.27...26.1.2-1.0.28) - 2026-09-05 13:12:07


### Changed
* Improve overall performance
  * Resolve value translators for round-tripped Graal proxies directly
  * Dispatch object value translators on their member key
  * Unwrap Graal proxies with instanceof instead of ClassCastException
  * Lazily populate idContext.ops in script contexts

### Fixed
* Register one script change listener per script instead of per error (#74), Closes #67
* Translate script values back to NBT when an operator expects NBT (#73), Closes #67
* Fix error when using regexes (#72), Closes #68
* Don't overwrite the exports binding when translating an NBT end tag
* Only temporarily set polyglot system props, Closes #66

<a name="26.1.2-1.0.27"></a>
## [26.1.2-1.0.27](https://github.com/CyclopsMC/IntegratedScripting/compare/26.1.2-1.0.26...26.1.2-1.0.27) - 2026-08-13 18:42:32 +0200


### Changed
* Update graal_version from 25.1.3 to 25.2.4

### Fixed
* Only temporarily set polyglot system props
  This fixes compat issues with Cobblemon, which package an older version
  of Graal that crashes on these system props.
  Closes #66

<a name="26.1.2-1.0.26"></a>
## [26.1.2-1.0.26](https://github.com/CyclopsMC/IntegratedScripting/compare/26.1.2-1.0.25...26.1.2-1.0.26) - 2026-08-04 07:44:11 +0200


### Changed
* Update to Graal 25.1.3

### Fixed
* Fix unable to run on some OSes such as OpenBSD

<a name="26.1.2-1.0.25"></a>
## [26.1.2-1.0.25](https://github.com/CyclopsMC/IntegratedScripting/compare/26.1.2-1.0.24...26.1.2-1.0.25) - 2026-07-28 16:57:46 +0200


### Fixed
* Don't crash when evaluating a non-existing script

<a name="26.1.2-1.0.24"></a>
## [26.1.2-1.0.24](https://github.com/CyclopsMC/IntegratedScripting/compare/26.1.2-1.0.23...26.1.2-1.0.24) - 2026-07-22 16:29:38 +0200


### Fixed
* Rewrite shadowing graal using fallback classloader
This commit has been cherry-picked from @wagyourtail's PR in #36.
Closes #63

<a name="26.1.2-1.0.23"></a>
## [26.1.2-1.0.23](https://github.com/CyclopsMC/IntegratedScripting/compare/26.1.2-1.0.22...26.1.2-1.0.23) - 2026-07-13 20:25:08 +0200


### Added
* New Translations (#65)

### Fixed
* Check for path safety when setting scripts

<a name="26.1.2-1.0.22"></a>
## [26.1.2-1.0.22](https://github.com/CyclopsMC/IntegratedScripting/compare/26.1.2-1.0.21...26.1.2-1.0.22) - 2026-05-16 10:22:16 +0200


### Fixed
* Fix external script edits not syncing when directory is created before disk (#62), Closes #61

<a name="26.1.2-1.0.21"></a>
## [26.1.2-1.0.21] - 2026-04-25 16:13:53 +0200


Initial 26.1.2 release
