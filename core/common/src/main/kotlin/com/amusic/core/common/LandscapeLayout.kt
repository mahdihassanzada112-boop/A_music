package com.amusic.core.common

/** The movable parts of the landscape player. [flexible] ones share a column's spare height. */
enum class LandscapePanel(val flexible: Boolean) {
    ARTWORK(true),
    INFO(false),
    PROGRESS(false),
    CONTROLS(false),
    LYRICS(true),
    VISUALIZER(true),
    QUEUE(true),
    EQUALIZER(true),
}

/** One panel in a column; [size] is its share of the height when it is flexible. */
data class PanelSlot(val panel: LandscapePanel, val size: Float = 1f)

/** [width] is the column's share of the screen's width. */
data class LayoutColumn(val panels: List<PanelSlot> = emptyList(), val width: Float = 1f)

/** A listener's own arrangement. Panels appear once at most; those not listed are hidden. */
data class LandscapeLayout(
    val columns: List<LayoutColumn>,
    val favorite: Boolean = true,
    val shuffle: Boolean = true,
    val repeat: Boolean = true,
    /** Off holds the studio background still in landscape. */
    val animatedBackground: Boolean = true,
) {
    val shown get() = columns.flatMap { column -> column.panels.map { it.panel } }
    val hidden get() = LandscapePanel.entries - shown.toSet()

    fun columnOf(panel: LandscapePanel) = columns.indexOfFirst { c -> c.panels.any { it.panel == panel } }

    fun hide(panel: LandscapePanel) =
        copy(columns = columns.map { c -> c.copy(panels = c.panels.filterNot { it.panel == panel }) })

    /**
     * Puts [panel] into [column] so that it ends up at [index] among the other panels there,
     * showing it if it was hidden.
     */
    fun place(panel: LandscapePanel, column: Int, index: Int): LandscapeLayout {
        if (column !in columns.indices) return this
        val slot = columns.flatMap { it.panels }.firstOrNull { it.panel == panel } ?: PanelSlot(panel)
        val without = hide(panel)
        return without.copy(
            columns = without.columns.mapIndexed { i, c ->
                if (i != column) c
                else c.copy(panels = c.panels.toMutableList().apply { add(index.coerceIn(0, size), slot) })
            },
        )
    }

    fun setWidth(column: Int, width: Float) = copy(
        columns = columns.mapIndexed { i, c -> if (i == column) c.copy(width = width.within(WIDTH)) else c },
    )

    fun setSize(panel: LandscapePanel, size: Float) = copy(
        columns = columns.map { c ->
            c.copy(panels = c.panels.map { if (it.panel == panel) it.copy(size = size.within(SIZE)) else it })
        },
    )

    /** A removed column hands its panels to the one before it. */
    fun withColumnCount(count: Int): LandscapeLayout {
        val wanted = count.coerceIn(COLUMNS)
        if (wanted >= columns.size) {
            return copy(columns = columns + List(wanted - columns.size) { LayoutColumn() })
        }
        val kept = columns.take(wanted)
        val moved = columns.drop(wanted).flatMap { it.panels }
        return copy(
            columns = kept.dropLast(1) + kept.last().let { it.copy(panels = it.panels + moved) },
        )
    }

    companion object {
        val COLUMNS = 2..3
        val WIDTH = 0.5f..2f
        val SIZE = 0.4f..2.5f

        private fun column(width: Float, vararg panels: LandscapePanel) =
            LayoutColumn(panels.map { PanelSlot(it) }, width)

        val TwoColumns = LandscapeLayout(
            listOf(
                column(0.9f, LandscapePanel.ARTWORK),
                column(1.1f, LandscapePanel.INFO, LandscapePanel.PROGRESS, LandscapePanel.CONTROLS),
            ),
        )

        val ThreeColumns = LandscapeLayout(
            listOf(
                column(0.9f, LandscapePanel.ARTWORK),
                column(1.2f, LandscapePanel.INFO, LandscapePanel.PROGRESS, LandscapePanel.CONTROLS),
                column(0.9f, LandscapePanel.QUEUE),
            ),
        )
    }
}

private fun Float.within(range: ClosedFloatingPointRange<Float>) = if (isNaN()) 1f else coerceIn(range)

/**
 * Layouts as one line of text: `1;fav=1;...;col=0.9:ARTWORK@1.0,INFO@1.0;col=...`.
 * Unknown or damaged parts are skipped.
 */
object LayoutCodec {
    private const val VERSION = "1"

    fun encode(layout: LandscapeLayout): String = buildList {
        add(VERSION)
        add("fav=${flag(layout.favorite)}")
        add("shuffle=${flag(layout.shuffle)}")
        add("repeat=${flag(layout.repeat)}")
        add("background=${flag(layout.animatedBackground)}")
        layout.columns.forEach { column ->
            add("col=${column.width}:" + column.panels.joinToString(",") { "${it.panel.name}@${it.size}" })
        }
    }.joinToString(";")

    /** null when [text] is not a usable layout. */
    fun decode(text: String?): LandscapeLayout? {
        val parts = text?.split(';') ?: return null
        if (parts.first() != VERSION) return null
        val flags = mutableMapOf<String, Boolean>()
        val columns = mutableListOf<LayoutColumn>()
        val used = mutableSetOf<LandscapePanel>()
        for (part in parts.drop(1)) {
            val key = part.substringBefore('=')
            val value = part.substringAfter('=', "")
            if (key != "col") {
                flags[key] = value != "0"
                continue
            }
            val panels = value.substringAfter(':', "").split(',').mapNotNull { entry ->
                val panel = LandscapePanel.entries.firstOrNull { it.name == entry.substringBefore('@') }
                    ?: return@mapNotNull null
                if (!used.add(panel)) return@mapNotNull null
                PanelSlot(panel, (entry.substringAfter('@', "").toFloatOrNull() ?: 1f).within(LandscapeLayout.SIZE))
            }
            val width = (value.substringBefore(':').toFloatOrNull() ?: 1f).within(LandscapeLayout.WIDTH)
            columns += LayoutColumn(panels, width)
        }
        if (columns.size !in LandscapeLayout.COLUMNS) return null
        return LandscapeLayout(
            columns,
            favorite = flags["fav"] ?: true,
            shuffle = flags["shuffle"] ?: true,
            repeat = flags["repeat"] ?: true,
            animatedBackground = flags["background"] ?: true,
        )
    }

    private fun flag(value: Boolean) = if (value) 1 else 0
}
