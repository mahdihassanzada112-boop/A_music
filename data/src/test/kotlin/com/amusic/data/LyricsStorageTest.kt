package com.amusic.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.amusic.core.database.AMusicDatabase
import com.amusic.core.database.MIGRATION_2_3
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LyricsStorageTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    /** Builds the database exactly as Phase 2 and 3 shipped it, from the exported schema. */
    private fun createVersion2(name: String) {
        val schema = JSONObject(
            File("../core/database/schemas/com.amusic.core.database.AMusicDatabase/2.json").readText(),
        ).getJSONObject("database")
        val file = context.getDatabasePath(name).also { it.parentFile!!.mkdirs() }
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            val entities = schema.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i)
                val table = entity.getString("tableName")
                fun run(sql: String) = db.execSQL(sql.replace("${'$'}{TABLE_NAME}", table))
                run(entity.getString("createSql"))
                val indices = entity.optJSONArray("indices") ?: continue
                for (j in 0 until indices.length()) run(indices.getJSONObject(j).getString("createSql"))
            }
            db.execSQL("INSERT INTO favorites (songId, addedAt) VALUES (7, 1)")
            db.execSQL("INSERT INTO playlists (id, name, createdAt) VALUES (1, 'mine', 1)")
            db.version = 2
        }
    }

    @Test fun upgradeKeepsUserDataAndAddsLyrics() = runBlocking {
        createVersion2("upgrade.db")
        val db = Room.databaseBuilder(context, AMusicDatabase::class.java, "upgrade.db")
            .addMigrations(MIGRATION_2_3)
            .allowMainThreadQueries()
            .build()
        try {
            assertEquals(listOf(7L), db.libraryDao().observeFavoriteIds().first())
            assertEquals("mine", db.libraryDao().observePlaylists().first().single().name)

            val lyrics = LyricsRepository(context, db.lyricsDao())
            assertNull(lyrics.observe(7).first())
            lyrics.save(7, "[00:01]a")
            lyrics.setOffset(7, 500_000)
            assertEquals(60_000L, lyrics.observe(7).first()!!.offsetMs)
            lyrics.save(7, "[00:02]b")
            assertEquals(0L, lyrics.observe(7).first()!!.offsetMs)
            lyrics.save(7, "  ")
            assertNull(lyrics.observe(7).first())
        } finally {
            db.close()
        }
    }
}
