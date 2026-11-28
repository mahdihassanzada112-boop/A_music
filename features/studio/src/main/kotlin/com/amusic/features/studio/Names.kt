package com.amusic.features.studio

import com.amusic.core.common.AutoBackground
import com.amusic.core.common.BackgroundPalette
import com.amusic.core.common.BackgroundStyle
import com.amusic.core.common.BuiltInPreset
import com.amusic.core.common.LookSetting

internal fun styleName(style: BackgroundStyle) = when (style) {
    BackgroundStyle.DYNAMIC -> R.string.bg_dynamic
    BackgroundStyle.BLUR -> R.string.bg_blur
    BackgroundStyle.SOLID -> R.string.bg_solid
    BackgroundStyle.ANIMATED -> R.string.bg_animated
    BackgroundStyle.MESH -> R.string.bg_mesh
    BackgroundStyle.PARTICLES -> R.string.bg_particles
    BackgroundStyle.AURORA -> R.string.bg_aurora
    BackgroundStyle.LIQUID -> R.string.bg_liquid
    BackgroundStyle.SPECTRUM -> R.string.bg_spectrum
    BackgroundStyle.WAVEFORM -> R.string.bg_waveform
    BackgroundStyle.GEOMETRIC -> R.string.bg_geometric
    BackgroundStyle.CINEMATIC -> R.string.bg_cinematic
    BackgroundStyle.ALBUM_3D -> R.string.bg_album_3d
    BackgroundStyle.SYNTHWAVE -> R.string.bg_synthwave
}

internal fun paletteName(palette: BackgroundPalette) = when (palette) {
    BackgroundPalette.AUTO -> R.string.palette_auto
    BackgroundPalette.ARTWORK -> R.string.palette_artwork
    BackgroundPalette.THEME -> R.string.palette_theme
    BackgroundPalette.SUNSET -> R.string.palette_sunset
    BackgroundPalette.OCEAN -> R.string.palette_ocean
    BackgroundPalette.FOREST -> R.string.palette_forest
    BackgroundPalette.NEON -> R.string.palette_neon
    BackgroundPalette.EMBER -> R.string.palette_ember
    BackgroundPalette.MONO -> R.string.palette_mono
    BackgroundPalette.CUSTOM -> R.string.palette_custom
}

internal fun settingName(setting: LookSetting) = when (setting) {
    LookSetting.BLUR -> R.string.look_blur
    LookSetting.BRIGHTNESS -> R.string.look_brightness
    LookSetting.SATURATION -> R.string.look_saturation
    LookSetting.CONTRAST -> R.string.look_contrast
    LookSetting.SPEED -> R.string.look_speed
    LookSetting.DENSITY -> R.string.look_density
    LookSetting.GLOW -> R.string.look_glow
    LookSetting.TRANSPARENCY -> R.string.look_transparency
    LookSetting.BASS -> R.string.look_bass
    LookSetting.BEAT -> R.string.look_beat
    LookSetting.MOTION -> R.string.look_motion
}

internal fun presetName(preset: BuiltInPreset) = when (preset) {
    BuiltInPreset.CONCERT -> R.string.preset_concert
    BuiltInPreset.NORTHERN_NIGHT -> R.string.preset_northern
    BuiltInPreset.NEON_DRIVE -> R.string.preset_neon
    BuiltInPreset.DEEP_WATER -> R.string.preset_water
    BuiltInPreset.GALLERY -> R.string.preset_gallery
    BuiltInPreset.SHAPES -> R.string.preset_shapes
    BuiltInPreset.CALM -> R.string.preset_calm
}

internal fun autoName(auto: AutoBackground) = when (auto) {
    AutoBackground.OFF -> R.string.auto_off
    AutoBackground.IN_ORDER -> R.string.auto_in_order
    AutoBackground.SHUFFLE -> R.string.auto_shuffle
}
