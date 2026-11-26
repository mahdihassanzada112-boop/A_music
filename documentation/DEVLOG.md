# Development log

## 2026-11-26 — Phase 1 (builds; device check pending)
Written: Gradle setup, 8 modules, 3-screen navigation, Room (songs table), DataStore
(appearance), Persian/English with RTL, unit tests, CI workflow.

Verified on GitHub Actions: unit tests (core/common, core/storage), lintDebug and
assembleDebug pass. Room schema version 1 is committed.

Not yet verified: installation and behaviour on a real device (navigation, language and
RTL switching, appearance persistence after restart, landscape). No instrumented tests.
