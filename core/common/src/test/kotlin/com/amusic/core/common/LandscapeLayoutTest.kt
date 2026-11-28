package com.amusic.core.common

import com.amusic.core.common.LandscapePanel.ARTWORK
import com.amusic.core.common.LandscapePanel.CONTROLS
import com.amusic.core.common.LandscapePanel.INFO
import com.amusic.core.common.LandscapePanel.LYRICS
import com.amusic.core.common.LandscapePanel.PROGRESS
import com.amusic.core.common.LandscapePanel.QUEUE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LandscapeLayoutTest {
    private val three = LandscapeLayout.ThreeColumns

    private fun LandscapeLayout.panels(column: Int) = columns[column].panels.map { it.panel }

    @Test fun panelsMoveWithinAndBetweenColumns() {
        val moved = three.place(CONTROLS, 1, 0)
        assertEquals(listOf(CONTROLS, INFO, PROGRESS), moved.panels(1))
        val across = moved.place(CONTROLS, 0, 5)
        assertEquals(listOf(ARTWORK, CONTROLS), across.panels(0))
        assertEquals(listOf(INFO, PROGRESS), across.panels(1))
        assertEquals(across, across.place(CONTROLS, 7, 0))
        assertEquals(across.shown.size, across.shown.toSet().size)
    }

    @Test fun panelsHideAndReturn() {
        val without = three.hide(QUEUE)
        assertTrue(QUEUE in without.hidden && without.columnOf(QUEUE) == -1)
        val back = without.place(LYRICS, 2, 0).place(QUEUE, 2, 1)
        assertEquals(listOf(LYRICS, QUEUE), back.panels(2))
    }

    @Test fun sizesStayWithPanelsAndInRange() {
        val sized = three.setSize(QUEUE, 2f).place(QUEUE, 0, 0)
        assertEquals(2f, sized.columns[0].panels[0].size)
        assertEquals(LandscapeLayout.SIZE.endInclusive, three.setSize(QUEUE, 99f).columns[2].panels[0].size)
        assertEquals(LandscapeLayout.WIDTH.start, three.setWidth(0, 0f).columns[0].width)
    }

    @Test fun columnCountKeepsEveryPanel() {
        val two = three.withColumnCount(2)
        assertEquals(2, two.columns.size)
        assertEquals(three.shown, two.shown)
        assertEquals(3, two.withColumnCount(3).columns.size)
        assertEquals(2, three.withColumnCount(1).columns.size)
    }

    @Test fun layoutsSurviveStorage() {
        val layout = three.setSize(ARTWORK, 1.5f).setWidth(2, 1.4f).hide(INFO)
            .copy(favorite = false, animatedBackground = false)
        assertEquals(layout, LayoutCodec.decode(LayoutCodec.encode(layout)))
        val empty = LandscapeLayout(listOf(LayoutColumn(), LayoutColumn()))
        assertEquals(empty, LayoutCodec.decode(LayoutCodec.encode(empty)))
    }

    @Test fun damagedLayoutsAreForgivenOrRejected() {
        assertNull(LayoutCodec.decode(null))
        assertNull(LayoutCodec.decode("2;col=1:"))
        assertNull(LayoutCodec.decode("1;col=1:ARTWORK"))
        val layout = LayoutCodec.decode("1;x=3;col=abc:ARTWORK@zz,GONE@1,ARTWORK;col=9:QUEUE@9")!!
        assertEquals(listOf(ARTWORK), layout.panels(0))
        assertEquals(1f, layout.columns[0].width)
        assertEquals(2f, layout.columns[1].width)
        assertEquals(2.5f, layout.columns[1].panels[0].size)
    }
}
