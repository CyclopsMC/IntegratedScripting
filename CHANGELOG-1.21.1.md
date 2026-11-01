# Changelog for Minecraft 1.21.1
All notable changes to this project will be documented in this file.

<a name="1.21.1-1.1.0"></a>
## [1.21.1-1.1.0](https://github.com/CyclopsMC/IntegratedScripting/compare/1.21.1-1.0.27...1.21.1-1.1.0) - 2026-11-01 11:31:19


### Changed
* Improve user experience of editor
  * Show a hint in the scripting terminal editor when no script is open
  * Color value types in script editor completions and signatures
  * Add a rebindable hotkey to enlarge the script editor
  * Open new scripts after creating them in the terminal
  * Hide JEI in the focus mode of the scripting terminal
  * Show syntax errors when hovering their scrollbar mark
  * Make the script editor scrollbar narrower
  * Show syntax error tooltips below their line
  * Add completion of idContext.ops in the script editor
  * Show the signature of idContext.ops calls in the script editor
  * Show the last evaluation of the bound card in the scripting terminal
  * Only bind variable cards to top-level declarations
  * Wrap long lines at spaces and punctuation in the script editor
  * Add a button to open the scripts folder of a disk
  * Add a focus mode to enlarge the script editor
  * Allow renaming scripts in the scripting terminal
  * Show full script paths when hovering the script list
  * Only re-wrap and re-highlight the editor text when it changes
  * Show syntax errors in the script editor
  * Document the script editor shortcuts in the info book
  * Improve syntax highlighting in the script editor
  * Add toolbar buttons for editor actions
  * Make the indent size of the script editor configurable
  * Scroll the script editor while drag-selecting
  * Add toggle comment and move lines to the script editor
  * Add find and go to line to the script editor
  * Highlight matching brackets in the script editor
  * Keep indentation on enter in the script editor
  * Indent with tab in the script editor
  * Notice all externally created script files
  * Only apply external changes when the file watcher reloads scripts
  * Show external script changes in the open editor
  * Write scripts to disk atomically
  * Add undo and redo to the script editor
  * Don't put the cursor on the hidden row below the script editor's last line
  * Avoid long disk labels to overlap with disk selector (#84)
  * Show what variable card would bind to (#83)
  * Keep the open script when the scripting terminal is resized
  * Make page up and page down move through the editor
  * Don't wrap editor text below the scrollbar
  * Start editor line numbers at 1
  * Keep trailing newlines when syncing scripts
  * Don't lose editor focus on unhandled keys
  * Keep the cursor visible in the editor
  * Don't reset the editor scroll position on every edit

### Fixed
* Fix empty scripting drives exposing a disk with ID -1
* Fix display panels not recovering from script errors after external edits (#91)
* Fix broken JavaScript examples in the info book
* Fix wrong operator names in the info book
* Fix the scripts directory path in the info book

<a name="1.21.1-1.0.27"></a>
## [1.21.1-1.0.27](https://github.com/CyclopsMC/IntegratedScripting/compare/1.21.1-1.0.26...1.21.1-1.0.27) - 2026-09-05 13:06:05 +0200


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

<a name="1.21.1-1.0.26"></a>
## [1.21.1-1.0.26](https://github.com/CyclopsMC/IntegratedScripting/compare/1.21.1-1.0.25...1.21.1-1.0.26) - 2026-08-13 18:39:13 +0200


### Changed
* Update graal_version from 25.1.3 to 25.2.4

### Fixed
* Only temporarily set polyglot system props
  This fixes compat issues with Cobblemon, which package an older version
  of Graal that crashes on these system props.
  Closes #66

<a name="1.21.1-1.0.25"></a>
## [1.21.1-1.0.25](https://github.com/CyclopsMC/IntegratedScripting/compare/1.21.1-1.0.24...1.21.1-1.0.25) - 2026-08-04 07:40:56 +0200


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
