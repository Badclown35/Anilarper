package app.anilarper.personal.data

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import app.anilarper.personal.Anime
import app.anilarper.personal.ReadingTitle

class AnimeLibraryViewModel(private val repository: LibraryRepository) : ViewModel() {
    private val _library = MutableStateFlow<List<Anime>>(emptyList())
    val library: StateFlow<List<Anime>> = _library.asStateFlow()
    private val writeMutex = Mutex()

    init {
        viewModelScope.launch {
            repository.observeAnime().collect { _library.value = it }
        }
    }

    fun toggle(anime: Anime) {
        viewModelScope.launch {
            writeMutex.withLock {
                val current = _library.value
                val updated = if (current.any { it.id == anime.id }) current.filterNot { it.id == anime.id }
                    else current + anime
                repository.saveAnimeToRoom(updated)
                _library.value = updated
            }
        }
    }

    fun update(anime: Anime) {
        viewModelScope.launch {
            writeMutex.withLock {
                val updated = _library.value.map { if (it.id == anime.id) anime else it }
                repository.saveAnimeToRoom(updated)
                _library.value = updated
            }
        }
    }

    fun remove(anime: Anime) {
        viewModelScope.launch {
            writeMutex.withLock {
                val updated = _library.value.filterNot { it.id == anime.id }
                repository.saveAnimeToRoom(updated)
                _library.value = updated
            }
        }
    }

    fun refresh() {
        viewModelScope.launch { repository.reloadRoomFromPreferences() }
    }
}

class ReadingLibraryViewModel(private val repository: LibraryRepository) : ViewModel() {
    private val _library = MutableStateFlow<List<ReadingTitle>>(emptyList())
    val library: StateFlow<List<ReadingTitle>> = _library.asStateFlow()
    private val writeMutex = Mutex()

    init {
        viewModelScope.launch {
            repository.observeReading().collect { _library.value = it }
        }
    }

    fun add(title: ReadingTitle) {
        viewModelScope.launch {
            writeMutex.withLock {
                if (_library.value.none { it.id == title.id && it.kind == title.kind }) {
                    val updated = _library.value + title
                    repository.saveReadingToRoom(updated)
                    _library.value = updated
                }
            }
        }
    }

    fun update(title: ReadingTitle) {
        viewModelScope.launch {
            writeMutex.withLock {
                val updated = _library.value.map {
                    if (it.id == title.id && it.kind == title.kind) title else it
                }
                repository.saveReadingToRoom(updated)
                _library.value = updated
            }
        }
    }

    fun remove(title: ReadingTitle) {
        viewModelScope.launch {
            writeMutex.withLock {
                val updated = _library.value.filterNot { it.id == title.id && it.kind == title.kind }
                repository.saveReadingToRoom(updated)
                _library.value = updated
            }
        }
    }

    fun refresh() {
        viewModelScope.launch { repository.reloadRoomFromPreferences() }
    }
}

class CollectionViewModelFactory(context: Context) : ViewModelProvider.Factory {
    private val repository = LibraryRepository(context.applicationContext)

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(AnimeLibraryViewModel::class.java) ->
            AnimeLibraryViewModel(repository) as T
        modelClass.isAssignableFrom(ReadingLibraryViewModel::class.java) ->
            ReadingLibraryViewModel(repository) as T
        else -> error("Unknown ViewModel class: ${modelClass.name}")
    }
}
