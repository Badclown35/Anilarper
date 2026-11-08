package app.anilarper.personal.data

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import app.anilarper.personal.Anime

data class DiscoverResultsCache(
    val items: List<Anime> = emptyList(),
    val page: Int = 1,
    val hasNextPage: Boolean = false
)

class DiscoverViewModel : ViewModel() {
    private val _cache = MutableStateFlow(DiscoverResultsCache())
    val cache: StateFlow<DiscoverResultsCache> = _cache.asStateFlow()

    fun updateCache(items: List<Anime>, page: Int, hasNextPage: Boolean) {
        _cache.value = DiscoverResultsCache(items, page, hasNextPage)
    }
}
