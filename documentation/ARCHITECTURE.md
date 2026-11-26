# Architecture

| Module | Responsibility |
|---|---|
| app | Entry point, navigation, wiring |
| core/common | Pure Kotlin types and utilities |
| core/design | Shell theme and shared components |
| core/database | Room database |
| core/storage | DataStore preferences |
| features/* | One screen area each, with its ViewModel |

Features depend on core modules, never on each other. Later modules (core/audio,
themes/*, player, lyrics, visualizer, rendering) are added in the phase that needs them.

## Decisions
- One playback engine and one playback state, outside every theme (Phase 2).
- Audio analysis and EQ run in a Media3 AudioProcessor chain; no microphone permission.
- The per-app language is owned by AppCompat, so it is not duplicated in DataStore.
- Room holds structured data and profiles; DataStore holds current selections.
- On large screens, orientation choice switches the in-app layout (the system may ignore locks).

## Phase order
1 Foundation · 2 Local playback and audio chain · 3 Theme contract + Dark Modern and
Glass UI (portrait and landscape) · 4 Lyrics and visualizers · 5 Background Studio ·
6 Landscape customization and equalizer · 7 Remaining 8 themes · 8 Video creator ·
9 Online music · 10 Final testing and release
