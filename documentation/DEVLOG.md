# Development log

## 2026-11-26 — Phase 1 (complete)
Written: Gradle setup, 8 modules, 3-screen navigation, Room (songs table), DataStore
(appearance), Persian/English with RTL, unit tests, CI workflow.

Verified on GitHub Actions: unit tests (core/common, core/storage), lintDebug and
assembleDebug pass. Room schema version 1 is committed.

Device check (reported by the owner, 2026-11-26): everything worked.
No instrumented tests yet.

## 2026-11-26 — Phase 2 (complete)
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

Verified on GitHub Actions (run 45794484663): 20 unit/Robolectric tests, lintDebug,
assembleDebug, and an emulator test on API 28 and 34 (launch, scan, play, next, pause,
saved queue, history).

Needs a real device: notification and lock screen controls, background playback, Bluetooth
and headphone events, audio interruptions, restore after restart, sleep timer, a large
library, and the screens in both languages.

Device check (reported by the owner, 2026-11-26): everything worked.

## 2026-11-27 — Phase 3 (complete)
Written: theme contract, Dark Modern and Glass UI (navigation, mini player, home, portrait
and landscape players), theme gallery with live previews, saved selection, reset, and
Auto / Portrait / Landscape orientation with a switch button on the player.

Not included yet: lyrics and visualizers (Phase 4), configurable backgrounds (Phase 5),
landscape customization and equalizer (Phase 6), the other eight themes (Phase 7).

Verified on GitHub Actions (run 45947049306): 24 unit/Robolectric tests, lintDebug,
assembleDebug, and two emulator tests on API 28 and 34. The new one opens both themes'
players, switches portrait/landscape and checks that the same queue keeps playing.

Fixed during verification: a type error in the sleep-timer flow, and a race in the playback
device test (it paused before the second song had started).

Needs a real device: appearance of both themes in both languages and light/dark, theme
gallery previews, landscape on small and large screens, and Auto rotation.

APK signed with the Phase 2 debug key built in run 45948415501.

Device check (reported by the owner, 2026-11-27): everything worked.

## 2026-11-27 — Phase 4 (built; device check pending)
- Lyrics: LRC and plain text, highlight and auto-scroll, tap a line to seek, import from a
  file (UTF-8, UTF-16 or Windows-1256), editor, timing shift, removal.
- Nine visualizer styles with color, intensity, speed and sensitivity settings.
- Not included: online lyrics providers and their cache (Phase 9), lyrics read from audio
  tags or from files beside the song, visualizers as backgrounds (Phase 5).

Verified on GitHub Actions: 37 unit/Robolectric tests (including the database 2 to 3
upgrade), lintDebug, assembleDebug, and three emulator tests on API 28 and 34. The new one
checks lyrics in both themes across rotation and that the audio tap hears a test tone.

Needs a real device: visualizer appearance and timing against the sound (also over
Bluetooth), battery use, importing real LRC files, Persian lyrics and keyboard entry.

