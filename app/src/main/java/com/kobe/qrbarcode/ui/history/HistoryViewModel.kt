package com.kobe.qrbarcode.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kobe.qrbarcode.core.model.CodeSource
import com.kobe.qrbarcode.data.repo.CodeRecord
import com.kobe.qrbarcode.data.repo.CodeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class HistoryFilter(val label: String) {
    ALL("All"),
    SCANNED("Scanned"),
    GENERATED("Created"),
    FAVOURITES("Favourites");

    val source: CodeSource?
        get() = when (this) {
            SCANNED -> CodeSource.SCANNED
            GENERATED -> CodeSource.GENERATED
            else -> null
        }
}

data class HistoryQuery(val filter: HistoryFilter = HistoryFilter.ALL, val query: String = "")

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: CodeRepository
) : ViewModel() {

    private val _query = MutableStateFlow(HistoryQuery())
    val query: StateFlow<HistoryQuery> = _query.asStateFlow()

    val records: StateFlow<List<CodeRecord>> = _query
        .flatMapLatest { current ->
            repository.filtered(
                source = current.filter.source,
                favouritesOnly = current.filter == HistoryFilter.FAVOURITES,
                query = current.query
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val counts: StateFlow<Triple<Int, Int, Int>> = combine(
        repository.scannedCount(),
        repository.generatedCount(),
        repository.favouriteCount()
    ) { scanned, generated, favourites -> Triple(scanned, generated, favourites) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Triple(0, 0, 0))

    fun setFilter(filter: HistoryFilter) = _query.update { it.copy(filter = filter) }

    fun setQuery(value: String) = _query.update { it.copy(query = value) }

    fun toggleFavorite(record: CodeRecord) {
        viewModelScope.launch { repository.setFavorite(record.id, !record.isFavorite) }
    }

    fun delete(record: CodeRecord) {
        viewModelScope.launch { repository.delete(record.id) }
    }

    fun clearAll() {
        viewModelScope.launch { repository.clearAll() }
    }
}
