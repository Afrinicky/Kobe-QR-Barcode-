package com.kobe.qrbarcode.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kobe.qrbarcode.data.repo.CodeRecord
import com.kobe.qrbarcode.data.repo.CodeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CodeDetailViewModel @Inject constructor(
    private val repository: CodeRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val id: Long = savedStateHandle.get<Long>("id") ?: 0L

    val record: StateFlow<CodeRecord?> = repository.observe(id)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun toggleFavorite() {
        val current = record.value ?: return
        viewModelScope.launch {
            repository.setFavorite(current.id, !current.isFavorite)
            _message.value = if (current.isFavorite) "Removed from favourites" else "Added to favourites"
        }
    }

    fun delete(onDeleted: () -> Unit) {
        val current = record.value ?: return
        viewModelScope.launch {
            repository.delete(current.id)
            onDeleted()
        }
    }

    fun showMessage(message: String) {
        _message.value = message
    }

    fun consumeMessage() {
        _message.value = null
    }
}
