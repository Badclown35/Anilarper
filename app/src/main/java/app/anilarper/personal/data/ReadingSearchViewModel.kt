package app.anilarper.personal.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.anilarper.personal.AniListReadingApi
import app.anilarper.personal.ReadingTitle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ReadingSearchState(
    val results: List<ReadingTitle> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null
)

class ReadingSearchViewModel : ViewModel() {
    private val _state = MutableStateFlow(ReadingSearchState())
    val state: StateFlow<ReadingSearchState> = _state.asStateFlow()
    private var searchJob: Job? = null

    fun search(query: String, novels: Boolean, cacheRepository: CatalogCacheRepository) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            val key = CatalogCacheRepository.readingKey(query, novels)
            try {
                val (aniListResult, mangaDexResult, kitsuResult) = coroutineScope {
                    val ani = async { runCatching { AniListReadingApi.search(query, novels) } }
                    val dex = async {
                        if (novels) Result.success(emptyList())
                        else runCatching { MangaDexApi.search(query) }
                    }
                    val kitsu = async {
                        if (novels) Result.success(emptyList())
                        else runCatching { KitsuMangaApi.search(query) }
                    }
                    Triple(ani.await(), dex.await(), kitsu.await())
                }
                val merged = mergeResults(
                    aniListResult.getOrDefault(emptyList()),
                    mangaDexResult.getOrDefault(emptyList()),
                    kitsuResult.getOrDefault(emptyList())
                )
                if (merged.isNotEmpty()) cacheRepository.saveReading(key, merged)
                val failed = buildList {
                    if (aniListResult.isFailure) add("AniList")
                    if (mangaDexResult.isFailure && !novels) add("MangaDex")
                    if (kitsuResult.isFailure && !novels) add("Kitsu")
                }
                val message = when {
                    failed.size == 3 -> "All catalogs are unavailable. Showing saved results if available."
                    failed.isNotEmpty() -> "${failed.joinToString()} unavailable — showing results from the other sources."
                    else -> null
                }
                if (merged.isNotEmpty()) {
                    _state.value = ReadingSearchState(results = merged, error = message)
                } else {
                    val cached = runCatching { cacheRepository.getReading(key) }.getOrNull()
                    _state.value = ReadingSearchState(
                        results = cached.orEmpty(),
                        error = message ?: if (cached.isNullOrEmpty()) "No matching titles found." else "Offline — showing saved results."
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                val cached = runCatching { cacheRepository.getReading(key) }.getOrNull()
                _state.value = ReadingSearchState(
                    results = cached.orEmpty(),
                    error = if (cached.isNullOrEmpty()) "Catalog unavailable. Check your connection and retry."
                    else "Offline — showing saved reading results."
                )
            }
        }
    }

    private fun mergeResults(
        aniList: List<ReadingTitle>,
        mangaDex: List<ReadingTitle>,
        kitsu: List<ReadingTitle>
    ): List<ReadingTitle> {
        val merged = LinkedHashMap<String, ReadingTitle>()
        (aniList + mangaDex + kitsu).forEach { title ->
            val normalized = title.title.lowercase().replace(Regex("[^\\p{L}\\p{N}]"), "")
            val key = normalized.ifBlank { "${title.provider}:${title.providerId}" }
            val existing = merged[key]
            // Prefer MangaDex for its source link, then Kitsu, while keeping AniList's metadata where available.
            merged[key] = when {
                existing == null -> title
                existing.provider == "MangaDex" -> existing
                title.provider == "MangaDex" -> title.copy(
                    chapters = existing.chapters ?: title.chapters,
                    volumes = existing.volumes ?: title.volumes,
                    description = title.description.ifBlank { existing.description }
                )
                existing.provider == "Kitsu" -> existing
                title.provider == "Kitsu" -> title.copy(
                    chapters = existing.chapters ?: title.chapters,
                    volumes = existing.volumes ?: title.volumes,
                    description = title.description.ifBlank { existing.description }
                )
                else -> existing
            }
        }
        return merged.values.take(150)
    }
}
