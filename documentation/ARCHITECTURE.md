# Architecture

| Module | Responsibility |
|---|---|
| app | Entry point, navigation, wiring |
| core/common | Pure Kotlin types and utilities |
| core/design | Shared components (artwork, song row) |
| core/database | Room database |
| core/storage | DataStore preferences and saved playback state |
| core/audio | The single player (MediaSessionService) and PlayerController |
| data | MediaStore scanning and the library repository |
| core/theming | Theme contract, UI models and shared player controls |
| themes/* | One visual identity each: navigation, mini player, home, two players |
| features/* | One screen area each, with its ViewModel |

Features depend on core modules, never on each other. Later modules (core/audio,
themes/*, player, lyrics, visualizer, rendering) are added in the phase that needs them.

## Decisions
- One playback engine and one playback state, outside every theme (Phase 2).
- Audio analysis and EQ run in a Media3 AudioProcessor chain; no microphone permission.
- The per-app language is owned by AppCompat, so it is not duplicated in DataStore.
- Room holds structured data and profiles; DataStore holds current selections.
- On large screens, orientation choice switches the in-app layout (the system may ignore locks).

## Lyrics and visualizers
Lyrics are typed or imported by the user and stored in Room (database version 3, with a real
migration). The player hands themes parsed lines; one shared panel draws them.

`AudioAnalyzer` receives a copy of the decoded audio from a Media3 `TeeAudioProcessor`. It
computes a spectrum only while a visualizer is on screen, and releases each result when that
audio becomes audible (`PlayoutQueue` models the output buffer, so timing is approximate).
Themes read it through the `AudioFeed` interface.

## Phase order
1 Foundation · 2 Local playback and audio chain · 3 Theme contract + Dark Modern and
Glass UI (portrait and landscape) · 4 Lyrics and visualizers · 5 Background Studio ·
6 Landscape customization and equalizer · 7 Remaining 8 themes · 8 Video creator ·
9 Online music · 10 Final testing and release

## Themes
A theme implements `PlayerTheme` and sees only `PlayerUi`, `PlayerActions`, `HomeUi` and
`HomeActions`, never the engine, so switching themes cannot affect playback. Library, search,
playlists and settings share one implementation styled by the theme's colors, type, shapes
and background. To add a theme: new module, new `ThemeId`, one line in `app/Themes.kt`.
