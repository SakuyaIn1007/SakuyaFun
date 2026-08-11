package com.sakuya.library.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sakuya.library.data.repository.LibraryRepository
import com.sakuya.library.model.LibraryItem
import com.sakuya.library.model.LibraryItemType
import com.sakuya.library.model.LibrarySyncStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: LibraryRepository
) : ViewModel() {
    /** 仓库直接暴露同步状态，书架 UI 可在不阻塞本地列表的情况下提示同步进度。 */
    val syncStatus: StateFlow<LibrarySyncStatus> = repository.syncStatus
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()


    val bookItems: StateFlow<List<LibraryItem>> =
        repository.observeBooks()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )
    val recentBookItems: StateFlow<List<LibraryItem>> =
        repository.observeRecentBooks()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )


    private val _isEditing = MutableStateFlow(false)
    val isEditing: StateFlow<Boolean> = _isEditing.asStateFlow()

    private val _selectedItemIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedItemIds: StateFlow<Set<String>> = _selectedItemIds.asStateFlow()

    private val _effects = MutableSharedFlow<LibraryEffect>()
    val effects = _effects.asSharedFlow()

    init {
        viewModelScope.launch {
            runCatching { repository.syncLibrary() }
                .onFailure { error ->
                    _effects.emit(LibraryEffect.ShowError(error.message ?: "同步书架失败"))
                }
        }
    }

    fun onAction(action: LibraryAction) {
        when (action) {
            is LibraryAction.SelectTab -> {
                _selectedTab.value = action.index
            }

            is LibraryAction.ToggleEdit -> {
                val nextEditing = !_isEditing.value
                _isEditing.value = nextEditing
                if (!nextEditing) {
                    _selectedItemIds.value = emptySet()
                }
            }

            is LibraryAction.ToggleItemSelected -> {
                _selectedItemIds.value = _selectedItemIds.value.toggle(action.id)
            }

            is LibraryAction.StartEditingWithItem -> {
                _isEditing.value = true
                _selectedItemIds.value = setOf(action.id)
            }

            is LibraryAction.DeleteSelectedItems -> {
                val selectedIds = _selectedItemIds.value
                if (selectedIds.isEmpty()) return
                viewModelScope.launch {
                    runCatching {
                        selectedIds.forEach { id -> repository.removeBook(id) }
                    }.onSuccess {
                        _selectedItemIds.value = emptySet()
                        _isEditing.value = false
                    }.onFailure { error ->
                        _effects.emit(LibraryEffect.ShowError(error.message ?: "删除书籍失败"))
                    }
                }
            }

            is LibraryAction.RemoveItem -> {
               viewModelScope.launch {
                   runCatching { repository.removeBook(action.id) }
                       .onFailure { error ->
                           _effects.emit(LibraryEffect.ShowError(error.message ?: "删除书籍失败"))
                       }
               }
            }

            is LibraryAction.ImportBook -> {
                viewModelScope.launch{
                    repository.importBook(
                        filePath = action.filePath,
                        title = action.title,
                        type = action.type
                    )
                }
            }

            is LibraryAction.OpenBook -> {
                val item = action.item
                if(!item.isValid()) return
                viewModelScope.launch {
                    _effects.emit(LibraryEffect.OpenReader(
                        bookId = item.id,
                        filePath = item.filePath
                    ))
                }
            }
        }
    }


}

sealed interface LibraryAction {
    data class ImportBook(
        val filePath: String,
        val title: String,
        val type: LibraryItemType
    ) : LibraryAction

    data class OpenBook(val item: LibraryItem) : LibraryAction
    data class RemoveItem(val id: String) : LibraryAction
    data class SelectTab(val index: Int) : LibraryAction
    data class ToggleItemSelected(val id: String) : LibraryAction
    data class StartEditingWithItem(val id: String) : LibraryAction
    data object DeleteSelectedItems : LibraryAction
    data object ToggleEdit : LibraryAction
}

sealed interface LibraryEffect {
    data class OpenReader(
        val bookId: String,
        val filePath: String,
    ) : LibraryEffect
    data class ShowError(val message: String) : LibraryEffect
}

private fun Set<String>.toggle(id: String): Set<String> {
    return if (id in this) {
        this - id
    } else {
        this + id
    }
}
