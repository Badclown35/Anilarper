package app.anilarper.personal.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import androidx.room.withTransaction
import kotlinx.coroutines.flow.map
import app.anilarper.personal.Anime
import app.anilarper.personal.ReadingTitle

/**
 * Single owner for the persisted anime and reading collections.
 * SharedPreferences remains the backing store during the Phase 2 refactor;
 * Phase 3 will migrate this repository to Room.
 */
class LibraryRepository(context: Context) {
    private val preferences = context.applicationContext
        .getSharedPreferences("anilarper", Context.MODE_PRIVATE)

    private val database = AnilarperDatabase.get(context.applicationContext)
    private val roomDao = database.collectionDao()

    fun observeAnime(): kotlinx.coroutines.flow.Flow<List<Anime>> =
        roomDao.observeCollection("anime").map { rows ->
            rows.mapNotNull { row -> runCatching { parseAnime(JSONObject(row.payload)) }.getOrNull() }
        }

    fun observeReading(): kotlinx.coroutines.flow.Flow<List<ReadingTitle>> =
        roomDao.observeCollection("reading").map { rows ->
            rows.mapNotNull { row -> runCatching { parseReading(JSONObject(row.payload)) }.getOrNull() }
        }

    suspend fun saveAnimeToRoom(items: List<Anime>) {
        database.withTransaction {
            roomDao.clearCollection("anime")
            roomDao.insertAll(items.mapIndexed { index, anime ->
                val payload = anime.toJson()
                CollectionEntity(anime.id.toString(), "anime", anime.id, "ANIME", index, payload.toString())
            })
        }
    }

    suspend fun saveReadingToRoom(items: List<ReadingTitle>) {
        database.withTransaction {
            roomDao.clearCollection("reading")
            roomDao.insertAll(items.mapIndexed { index, item ->
                val payload = item.toJson()
                CollectionEntity("${item.kind}:${item.id}", "reading", item.id, item.kind, index, payload.toString())
            })
        }
    }

    suspend fun reloadRoomFromPreferences() {
        saveAnimeToRoom(loadAnime())
        saveReadingToRoom(loadReading())
    }

    private fun parseAnime(o: JSONObject) = Anime(
        o.getInt("id"), o.getString("title"), o.optString("nativeTitle"), o.optString("cover"),
        o.optString("format"), o.optString("status"),
        if (o.isNull("episodes")) null else o.optInt("episodes"),
        if (o.isNull("score")) null else o.optInt("score"), o.optString("description"),
        o.optString("watchStatus", "PLANNING"), o.optInt("progress", 0), o.optLong("lastWatchedAt", 0L),
        o.optString("season"),
        if (!o.has("seasonYear") || o.isNull("seasonYear")) null else o.optInt("seasonYear"),
        o.optJSONArray("genres").toStrings(), o.optJSONArray("studios").toStrings()
    )

    private fun parseReading(o: JSONObject) = ReadingTitle(
        o.getInt("id"), o.getString("title"), o.optString("nativeTitle"), o.optString("cover"),
        o.optString("format"), o.optString("status"),
        if (o.isNull("chapters")) null else o.optInt("chapters"),
        if (o.isNull("volumes")) null else o.optInt("volumes"), o.optString("description"),
        o.optString("kind", "MANGA"), o.optInt("progress"), o.optInt("volumeProgress"),
        o.optString("readingStatus", "PLANNING")
    )

    private fun Anime.toJson() = JSONObject()
        .put("id", id).put("title", title).put("nativeTitle", nativeTitle)
        .put("cover", cover).put("format", format).put("status", status)
        .put("episodes", episodes).put("score", score).put("description", description)
        .put("watchStatus", watchStatus).put("progress", progress).put("lastWatchedAt", lastWatchedAt)
        .put("season", season).put("seasonYear", seasonYear)
        .put("genres", JSONArray(genres)).put("studios", JSONArray(studios))

    private fun ReadingTitle.toJson() = JSONObject()
        .put("id", id).put("title", title).put("nativeTitle", nativeTitle)
        .put("cover", cover).put("format", format).put("status", status)
        .put("chapters", chapters).put("volumes", volumes).put("description", description)
        .put("kind", kind).put("progress", progress).put("volumeProgress", volumeProgress)
        .put("readingStatus", readingStatus)

    fun loadAnime(): List<Anime> = readArray("library")?.mapNotNull { o ->
        runCatching {
            Anime(
                o.getInt("id"), o.getString("title"), o.optString("nativeTitle"), o.optString("cover"),
                o.optString("format"), o.optString("status"),
                if (o.isNull("episodes")) null else o.optInt("episodes"),
                if (o.isNull("score")) null else o.optInt("score"), o.optString("description"),
                o.optString("watchStatus", "PLANNING"), o.optInt("progress", 0), o.optLong("lastWatchedAt", 0L),
                o.optString("season"),
                if (!o.has("seasonYear") || o.isNull("seasonYear")) null else o.optInt("seasonYear"),
                o.optJSONArray("genres").toStrings(), o.optJSONArray("studios").toStrings()
            )
        }.getOrNull()
    } ?: emptyList()

    fun saveAnime(items: List<Anime>) {
        val array = JSONArray()
        items.forEach { anime ->
            array.put(JSONObject()
                .put("id", anime.id).put("title", anime.title).put("nativeTitle", anime.nativeTitle)
                .put("cover", anime.cover).put("format", anime.format).put("status", anime.status)
                .put("episodes", anime.episodes).put("score", anime.score).put("description", anime.description)
                .put("watchStatus", anime.watchStatus).put("progress", anime.progress)
                .put("lastWatchedAt", anime.lastWatchedAt).put("season", anime.season)
                .put("seasonYear", anime.seasonYear).put("genres", JSONArray(anime.genres))
                .put("studios", JSONArray(anime.studios)))
        }
        check(preferences.edit().putString("library", array.toString()).commit()) {
            "Couldn't save anime library."
        }
    }

    fun loadReading(): List<ReadingTitle> = readArray("readingLibrary")?.mapNotNull { o ->
        runCatching {
            ReadingTitle(
                o.getInt("id"), o.getString("title"), o.optString("nativeTitle"), o.optString("cover"),
                o.optString("format"), o.optString("status"),
                if (o.isNull("chapters")) null else o.optInt("chapters"),
                if (o.isNull("volumes")) null else o.optInt("volumes"), o.optString("description"),
                o.optString("kind", "MANGA"), o.optInt("progress"), o.optInt("volumeProgress"),
                o.optString("readingStatus", "PLANNING")
            )
        }.getOrNull()
    } ?: emptyList()

    fun saveReading(items: List<ReadingTitle>) {
        val array = JSONArray()
        items.forEach { item ->
            array.put(JSONObject()
                .put("id", item.id).put("title", item.title).put("nativeTitle", item.nativeTitle)
                .put("cover", item.cover).put("format", item.format).put("status", item.status)
                .put("chapters", item.chapters).put("volumes", item.volumes)
                .put("description", item.description).put("kind", item.kind)
                .put("progress", item.progress).put("volumeProgress", item.volumeProgress)
                .put("readingStatus", item.readingStatus))
        }
        check(preferences.edit().putString("readingLibrary", array.toString()).commit()) {
            "Couldn't save reading library."
        }
    }

    private fun readArray(key: String): List<JSONObject>? {
        val raw = preferences.getString(key, "[]") ?: "[]"
        val array = runCatching { JSONArray(raw) }.getOrElse {
            preferences.edit().putString("${key}_corrupt_backup", raw).apply()
            return null
        }
        return (0 until array.length()).mapNotNull { index ->
            runCatching { array.getJSONObject(index) }.getOrNull()
        }
    }

    private fun JSONArray?.toStrings(): List<String> =
        if (this == null) emptyList()
        else (0 until length()).mapNotNull { optString(it).takeIf(String::isNotBlank) }
}
