package com.amusic.core.common

/**
 * Scenes as one line of text, for storage. Reading is forgiving: unknown styles, settings or
 * damaged parts are skipped, so a preset from another app version still loads.
 *
 * `1;palette=ARTWORK;BLUR=0.7;layer=MESH,1,0.6,1.0,1.0,AUTO,ff2b2150;layer=...`
 */
object BackgroundCodec {
    private const val VERSION = "1"

    fun encode(scene: BackgroundScene): String = buildList {
        add(VERSION)
        add("palette=${scene.palette.name}")
        LookSetting.entries.forEach {
            if (scene.look[it] != it.default) add("${it.name}=${scene.look[it]}")
        }
        scene.layers.forEach {
            val color = Integer.toHexString(it.color)
            add(
                "layer=${it.style.name},${if (it.enabled) 1 else 0},${it.opacity},${it.intensity}," +
                    "${it.speed},${it.palette.name},$color",
            )
        }
    }.joinToString(";")

    /** null when [text] is not a scene at all. */
    fun decode(text: String?): BackgroundScene? {
        val parts = text?.split(';') ?: return null
        if (parts.first() != VERSION) return null
        var look = BackgroundLook()
        var palette = BackgroundPalette.ARTWORK
        val layers = mutableListOf<BackgroundLayer>()
        for (part in parts.drop(1)) {
            val key = part.substringBefore('=')
            val value = part.substringAfter('=', "")
            when (key) {
                "palette" -> palette = paletteNamed(value) ?: palette
                "layer" -> decodeLayer(value)?.let { layer ->
                    if (layers.size < BackgroundScene.MAX_LAYERS && layers.none { it.style == layer.style }) {
                        layers += layer
                    }
                }
                else -> {
                    val setting = LookSetting.entries.firstOrNull { it.name == key }
                    val number = value.toFloatOrNull()
                    if (setting != null && number != null) look = look.with(setting, number)
                }
            }
        }
        return BackgroundScene(layers, look, palette)
    }

    private fun decodeLayer(text: String): BackgroundLayer? {
        val fields = text.split(',')
        val style = BackgroundStyle.entries.firstOrNull { it.name == fields[0] } ?: return null
        val base = BackgroundScene.defaultLayer(style)
        return BackgroundLayer(
            style = style,
            enabled = fields.getOrNull(1) != "0",
            opacity = fields.getOrNull(2)?.toFloatOrNull() ?: base.opacity,
            intensity = fields.getOrNull(3)?.toFloatOrNull() ?: base.intensity,
            speed = fields.getOrNull(4)?.toFloatOrNull() ?: base.speed,
            palette = fields.getOrNull(5)?.let(::paletteNamed) ?: base.palette,
            color = fields.getOrNull(6)?.toLongOrNull(16)?.toInt() ?: base.color,
        ).clamped()
    }

    private fun paletteNamed(name: String) = BackgroundPalette.entries.firstOrNull { it.name == name }
}
