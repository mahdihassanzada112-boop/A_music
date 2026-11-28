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

## Background Studio
- A `BackgroundScene` (core/common) is up to six layers, one per style, plus shared settings
  and a palette. It is stored as one line of text (`BackgroundCodec`), which skips anything
  it does not know.
- `BackgroundStage` (core/theming) draws a scene on a Compose canvas. Nothing in it knows
  about the player beyond the cover, a running flag and the `AudioFeed`.
- A new style needs an entry in `BackgroundStyle`, a drawing function in
  `BackgroundDrawing.kt` and a name in the studio's strings.
- Frames are drawn only while the screen is visible, up to the chosen limit; low-memory
  devices, battery saver and the system power saver lower detail and frame rate. The loop
  stops once paused music has faded out, and still layers need no loop at all.
- Blur is computed once per cover on a small copy, so it works the same from Android 8.
- Saved presets are in the database (version 4). Changing background with the song happens
  in `MainViewModel`.

## Landscape layouts
- A `LandscapeLayout` (core/common) is two or three columns of panels, a width per column, a
  height share per tall panel, and switches for favorite, shuffle, repeat and the moving
  background. `LayoutCodec` stores it as one line and skips anything it does not know.
- Each theme's current layout is a setting of its own; without one the theme draws its own
  landscape player. Named layouts are in the database (version 5), also per theme.
- `CustomLandscape` (core/theming) places the panels. A theme draws the artwork, song
  information, progress bar and controls (`LandscapeBlock`) and the surface behind the shared
  lyrics, queue, visualizer and equalizer panels (`LandscapePane`). The bar on top is fixed,
  so the listener cannot remove the way back.
- The editor (features/customize) changes the stored layout directly; dragging and the panel
  menus use the same operations.

## Equalizer
- The app processes the audio itself (`EqualizerDsp`, core/common) instead of using the
  device's audio effects, which differ between manufacturers and are sometimes missing.
- `EqualizerProcessor` sits in the player's audio sink ahead of the visualizers' tap and works
  on 16-bit audio; the sink is not asked for float output.
- Ten peaking bands, a bass shelf and mid/side stereo widening. Boosts lower the overall
  level by the same peak amount, and a soft knee catches the rest. Changes glide in.
- Settings are in DataStore, named curves in the database. The playback service applies them
  and watches for headphones; it cannot tell a Bluetooth speaker from Bluetooth headphones.

