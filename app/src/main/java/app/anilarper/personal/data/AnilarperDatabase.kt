package app.anilarper.personal.data

import android.content.ContentValues
import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray

@Entity(tableName = "collection_items")
data class CollectionEntity(
    @PrimaryKey val key: String,
    val collectionType: String,
    val itemId: Int,
    val kind: String,
    val position: Int,
    val payload: String
)

@Dao
interface CollectionDao {
    @Query("SELECT * FROM collection_items WHERE collectionType = :type ORDER BY position ASC")
    fun observeCollection(type: String): Flow<List<CollectionEntity>>

    @Query("SELECT * FROM collection_items WHERE collectionType = :type ORDER BY position ASC")
    suspend fun getCollectionOnce(type: String): List<CollectionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CollectionEntity>)

    @Query("DELETE FROM collection_items WHERE collectionType = :type")
    suspend fun clearCollection(type: String)
}

@Database(entities = [CollectionEntity::class], version = 1, exportSchema = false)
abstract class AnilarperDatabase : RoomDatabase() {
    abstract fun collectionDao(): CollectionDao

    companion object {
        @Volatile private var INSTANCE: AnilarperDatabase? = null

        fun get(context: Context): AnilarperDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AnilarperDatabase::class.java,
                    "anilarper.db"
                ).addCallback(SharedPreferencesImportCallback(context.applicationContext))
                    .build()
                    .also { INSTANCE = it }
            }
    }
}

/**
 * One-time import for existing installs. Each JSON entry is handled independently,
 * so a malformed row cannot prevent the remaining collection from migrating.
 * The original preferences are deliberately left untouched as a recovery source.
 */
private class SharedPreferencesImportCallback(
    private val context: Context
) : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        val preferences = context.getSharedPreferences("anilarper", Context.MODE_PRIVATE)
        if (preferences.getBoolean("roomMigrationV1Complete", false)) return

        importArray(db, preferences.getString("library", "[]"), "anime")
        importArray(db, preferences.getString("readingLibrary", "[]"), "reading")
        preferences.edit().putBoolean("roomMigrationV1Complete", true).commit()
    }

    private fun importArray(db: SupportSQLiteDatabase, raw: String?, type: String) {
        val array = runCatching { JSONArray(raw ?: "[]") }.getOrNull() ?: return
        for (index in 0 until array.length()) {
            val item = runCatching { array.optJSONObject(index) }.getOrNull() ?: continue
            val id = item.optInt("id", -1)
            if (id <= 0) continue
            val kind = if (type == "reading") item.optString("kind", "MANGA") else "ANIME"
            val key = if (type == "reading") "$kind:$id" else id.toString()
            runCatching {
                val values = ContentValues().apply {
                    put("key", key)
                    put("collectionType", type)
                    put("itemId", id)
                    put("kind", kind)
                    put("position", index)
                    put("payload", item.toString())
                }
                db.insert("collection_items", 5, values)
            }
        }
    }
}
