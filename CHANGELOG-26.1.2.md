# Changelog for Minecraft 26.1.2
All notable changes to this project will be documented in this file.

<a name="26.1.2-1.1.0"></a>
## [26.1.2-1.1.0](https://github.com/CyclopsMC/IntegratedScripting/compare/26.1.2-1.0.28...26.1.2-1.1.0) - 2026-11-01 11:31:54


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

<a name="26.1.2-1.0.28"></a>
## [26.1.2-1.0.28](https://github.com/CyclopsMC/IntegratedScripting/compare/26.1.2-1.0.27...26.1.2-1.0.28) - 2026-09-05 13:12:07 +0200


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
