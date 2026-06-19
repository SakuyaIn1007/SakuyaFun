package com.sakuya.library.viewmodel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sakuya.library.model.LibraryItem
import com.sakuya.library.model.LibraryItemType
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor() : ViewModel() {

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _bookItems = MutableStateFlow(sampleBookItems())
    val bookItems: StateFlow<List<LibraryItem>> = _bookItems.asStateFlow()

    private val _isEditing = MutableStateFlow(false)
    val isEditing: StateFlow<Boolean> = _isEditing.asStateFlow()
    private val _events = MutableSharedFlow<LibraryEvent>()
    val events = _events.asSharedFlow()

    fun onAction(action: LibraryAction) {
        when (action) {
            is LibraryAction.SelectTab -> {
                _selectedTab.value = action.index
            }

            LibraryAction.ToggleEdit -> {
                _isEditing.value = !_isEditing.value
            }

            is LibraryAction.RemoveItem -> {
                _bookItems.value = _bookItems.value.filter { it.id != action.id }
            }

            is LibraryAction.ImportBook -> {
                val importedBook = LibraryItem(
                    id = action.filePath,
                    title = action.title.ifBlank { "未命名书籍" },
                    subtitle = when (action.type) {
                        LibraryItemType.TXT -> "本地 TXT"
                        LibraryItemType.EPUB -> "本地 EPUB"
                    },
                    rating = 0f,
                    tags = listOf("本地导入"),
                    type = action.type,
                    collectedAt = currentDateTime(),
                    filePath = action.filePath
                )
                _bookItems.value = listOf(importedBook) + _bookItems.value.filter {
                    it.filePath != action.filePath
                }
            }

            is LibraryAction.OpenBook -> {
                val filePath = action.item.filePath
                if (filePath.isNotEmpty()) {
                    viewModelScope.launch {
                        _events.emit(LibraryEvent.OpenReader(filePath))
                    }
                }
            }
        }
    }


}

private fun currentDateTime(): String {
    return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
}

private fun sampleBookItems() = listOf(
    LibraryItem(
        id = "novel_1",
        title = "刀剑神域",
        subtitle = "川原砾 · 电击文库",
        rating = 4.8f,
        tags = listOf("科幻", "冒险", "虚拟现实"),
        type = LibraryItemType.TXT,
        collectedAt = "2026-06-01"
    ),
    LibraryItem(
        id = "novel_2",
        title = "关于我转生变成史莱姆这档事",
        subtitle = "伏濑 · GC Novels",
        rating = 4.6f,
        tags = listOf("异世界", "奇幻", "轻松"),
        type = LibraryItemType.TXT,
        collectedAt = "2026-05-28"
    ),
    LibraryItem(
        id = "novel_3",
        title = "Re:从零开始的异世界生活",
        subtitle = "长月达平 · MF文库J",
        rating = 4.7f,
        tags = listOf("异世界", "悬疑", "轮回"),
        type = LibraryItemType.TXT,
        collectedAt = "2026-05-20"
    ),
    LibraryItem(
        id = "novel_4",
        title = "无职转生",
        subtitle = "理不尽な孫の手 · MF Books",
        rating = 4.5f,
        tags = listOf("异世界", "成长", "冒险"),
        type = LibraryItemType.TXT,
        collectedAt = "2026-05-15"
    ),
    LibraryItem(
        id = "novel_5",
        title = "86-不存在的战区",
        subtitle = "安里アサト · 电击文库",
        rating = 4.8f,
        tags = listOf("科幻", "战争", "机甲"),
        type = LibraryItemType.TXT,
        collectedAt = "2026-05-10"
    ),
    LibraryItem(
        id = "novel_6",
        title = "狼与香辛料",
        subtitle = "支仓冻砂 · 电击文库",
        rating = 4.7f,
        tags = listOf("冒险", "经商", "奇幻"),
        type = LibraryItemType.TXT,
        collectedAt = "2026-05-05"
    ),
    LibraryItem(
        id = "novel_7",
        title = "OVERLORD",
        subtitle = "丸山くがね · Enterbrain",
        rating = 4.7f,
        tags = listOf("异世界", "黑暗", "奇幻"),
        type = LibraryItemType.TXT,
        collectedAt = "2026-04-28"
    ),
    LibraryItem(
        id = "novel_8",
        title = "青春猪头少年系列",
        subtitle = "鸭志田一 · 电击文库",
        rating = 4.6f,
        tags = listOf("恋爱", "青春", "校园"),
        type = LibraryItemType.TXT,
        collectedAt = "2026-04-20"
    ),
)

sealed interface LibraryAction {
    data class ImportBook(
        val filePath: String,
        val title: String,
        val type: LibraryItemType
    ) : LibraryAction

    data class OpenBook(val item: LibraryItem) : LibraryAction
    data class RemoveItem(val id: String) : LibraryAction
    data class SelectTab(val index: Int) : LibraryAction
    data object ToggleEdit : LibraryAction
}

sealed interface LibraryEvent {
    data class OpenReader(val filePath: String) : LibraryEvent
}
