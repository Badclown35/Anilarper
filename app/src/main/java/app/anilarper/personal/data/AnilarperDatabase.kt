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
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray

@Entity(tableName = "catalog_cache")
data class CatalogCacheEntity(
    @PrimaryKey val cacheKey: String,
    val payload: String,
    val cachedAt: Long
)

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

    @Query("SELECT * FROM catalog_cache WHERE cacheKey = :key LIMIT 1")
    suspend fun getCached(key: String): CatalogCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putCached(item: CatalogCacheEntity)

    @Query("DELETE FROM catalog_cache WHERE cachedAt < :cutoff")
    suspend fun pruneCache(cutoff: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CollectionEntity>)

    @Query("DELETE FROM collection_items WHERE collectionType = :type")
    suspend fun clearCollection(type: String)
}

@Database(entities = [CollectionEntity::class, CatalogCacheEntity::class], version = 2, exportSchema = false)
abstract class AnilarperDatabase : RoomDatabase() {
    abstract fun collectionDao(): CollectionDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS catalog_cache (cacheKey TEXT NOT NULL, payload TEXT NOT NULL, cachedAt INTEGER NOT NULL, PRIMARY KEY(cacheKey))")
            }
        }

        @Volatile private var INSTANCE: AnilarperDatabase? = null

        fun get(context: Context): AnilarperDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AnilarperDatabase::class.java,
                    "anilarper.db"
                ).addMigrations(MIGRATION_1_2)
                    .addCallback(SharedPreferencesImportCallback(context.applicationContext))
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

        importArray(db, preferences.getString("library", "[]"), "anime")
        importArray(db, preferences.getString("readingLibrary", "[]"), "reading")
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
