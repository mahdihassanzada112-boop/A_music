package com.amusic.data

import com.amusic.core.common.LandscapeLayout
import com.amusic.core.common.LayoutCodec
import com.amusic.core.common.ThemeId
import com.amusic.core.common.decodeBands
import com.amusic.core.common.encodeBands
import com.amusic.core.database.EqPresetEntity
import com.amusic.core.database.LayoutProfileEntity
import com.amusic.core.database.ProfileDao
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class LayoutProfile(val id: Long, val name: String, val layout: LandscapeLayout)

data class EqCurve(val id: Long, val name: String, val bands: List<Float>)

/** Saved landscape layouts and equalizer curves. A blank name saves nothing. */
@Singleton
class ProfileRepository @Inject constructor(private val dao: ProfileDao) {
    fun layouts(theme: ThemeId): Flow<List<LayoutProfile>> = dao.observeLayouts(theme.name).map { rows ->
        rows.mapNotNull { row -> LayoutCodec.decode(row.layout)?.let { LayoutProfile(row.id, row.name, it) } }
    }

    suspend fun saveLayout(theme: ThemeId, name: String, layout: LandscapeLayout) {
        val clean = clean(name) ?: return
        dao.insert(LayoutProfileEntity(theme = theme.name, name = clean, layout = LayoutCodec.encode(layout)))
    }

    suspend fun deleteLayout(id: Long) = dao.deleteLayout(id)

    val curves: Flow<List<EqCurve>> = dao.observeCurves().map { rows ->
        rows.mapNotNull { row -> decodeBands(row.bands)?.let { EqCurve(row.id, row.name, it) } }
    }

    suspend fun saveCurve(name: String, bands: List<Float>) {
        val clean = clean(name) ?: return
        dao.insert(EqPresetEntity(name = clean, bands = encodeBands(bands)))
    }

    suspend fun deleteCurve(id: Long) = dao.deleteCurve(id)

    private fun clean(name: String) = name.trim().take(PresetRepository.MAX_NAME).ifEmpty { null }
}
