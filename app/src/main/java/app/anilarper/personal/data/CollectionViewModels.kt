package app.anilarper.personal.data

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import app.anilarper.personal.Anime
import app.anilarper.personal.ReadingTitle

class AnimeLibraryViewModel(private val repository: LibraryRepository) : ViewModel() {
    private val _library = MutableStateFlow(repository.loadAnime())
    val library: StateFlow<List<Anime>> = _library.asStateFlow()

    fun toggle(anime: Anime) {
        val current = _library.value
        val updated = if (current.any { it.id == anime.id }) current.filterNot { it.id == anime.id }
            else current + anime
        save(updated)
    }

    fun update(anime: Anime) {
        save(_library.value.map { if (it.id == anime.id) anime else it })
    }

    fun remove(anime: Anime) {
        save(_library.value.filterNot { it.id == anime.id })
    }

    fun refresh() {
        _library.value = repository.loadAnime()
    }

    private fun save(items: List<Anime>) {
        repository.saveAnime(items)
        _library.value = items
    }
}

class ReadingLibraryViewModel(private val repository: LibraryRepository) : ViewModel() {
    private val _library = MutableStateFlow(repository.loadReading())
    val library: StateFlow<List<ReadingTitle>> = _library.asStateFlow()

    fun add(title: ReadingTitle) {
        if (_library.value.none { it.id == title.id && it.kind == title.kind }) {
            save(_library.value + title)
        }
    }

    fun update(title: ReadingTitle) {
        save(_library.value.map {
            if (it.id == title.id && it.kind == title.kind) title else it
        })
    }

    fun remove(title: ReadingTitle) {
        save(_library.value.filterNot { it.id == title.id && it.kind == title.kind })
    }

    fun refresh() {
        _library.value = repository.loadReading()
    }

    private fun save(items: List<ReadingTitle>) {
        repository.saveReading(items)
        _library.value = items
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
