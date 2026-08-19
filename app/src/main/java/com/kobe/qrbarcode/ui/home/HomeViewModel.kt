package com.kobe.qrbarcode.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kobe.qrbarcode.data.repo.CodeRecord
import com.kobe.qrbarcode.data.repo.CodeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val recent: List<CodeRecord> = emptyList(),
    val scannedCount: Int = 0,
    val generatedCount: Int = 0,
    val favouriteCount: Int = 0
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: CodeRepository
) : ViewModel() {

    val state: StateFlow<HomeUiState> = combine(
        repository.recent(5),
        repository.scannedCount(),
        repository.generatedCount(),
        repository.favouriteCount()
    ) { recent, scanned, generated, favourites ->
        HomeUiState(recent, scanned, generated, favourites)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun toggleFavorite(record: CodeRecord) {
        viewModelScope.launch { repository.setFavorite(record.id, !record.isFavorite) }
    }
}
