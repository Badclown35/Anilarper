package app.anilarper.personal.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.anilarper.personal.AniListReadingApi
import app.anilarper.personal.ReadingTitle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
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

    fun search(query: String, novels: Boolean) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            try {
                val results = AniListReadingApi.search(query, novels)
                _state.value = ReadingSearchState(results = results)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _state.value = _state.value.copy(
                    loading = false,
                    error = "Catalog unavailable. Check your connection and retry."
                )
            }
        }
    }
}
