# Development log

## 2026-11-26 — Phase 1 (complete)
Written: Gradle setup, 8 modules, 3-screen navigation, Room (songs table), DataStore
(appearance), Persian/English with RTL, unit tests, CI workflow.

Verified on GitHub Actions: unit tests (core/common, core/storage), lintDebug and
assembleDebug pass. Room schema version 1 is committed.

Device check (reported by the owner, 2026-11-26): everything worked.
No instrumented tests yet.

## 2026-11-26 — Phase 2 (in progress)
Written: library scan with live updates, legacy Persian tag repair, Room v2, all library
sections, search, sort, multi-select, playlists, favorites, history, duplicates, one Media3
player in a MediaSessionService, queue, shuffle, repeat, speed, sleep timer, audio focus,
headphone unplug, state restore, mini player and a plain Now Playing screen.

Not included yet, with reasons:
- Albums, artists and genres are derived from songs; separate tables are not needed so far.
- The audio processing tap is added with its first consumer (visualizer, Phase 4).
- Restarting playback from a Bluetooth button after the app process has ended.
- With shuffle on, "Play next" does not guarantee the item plays next.
- Sleep timer has fixed durations only. Equalizer is Phase 6.
