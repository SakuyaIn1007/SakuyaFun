package com.sakuya.reader.viewmodel

import androidx.lifecycle.ViewModel
import com.sakuya.reader.data.EpubLoader
import com.sakuya.reader.data.TxtLoader
import com.sakuya.reader.model.ReaderType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import java.io.Reader
import javax.inject.Inject


@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val epubLoader: EpubLoader,
    private val txtLoader: TxtLoader
) : ViewModel(){

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState = _uiState.asStateFlow()

    fun openFile(file: File){
        when (file.extension.lowercase()){
            "txt" -> loadTxt(file)
            "epub" -> loadEpub(file)
            else -> error("错误的格式")
        }
    }

    private fun loadTxt(file: File){
        val pages = txtLoader.load(file)
        _uiState.update {
            it.copy(
                type = ReaderType.TXT,
                pages = pages,
                currentPage = 0
            )
        }
    }

    private fun loadEpub(file: File){
        val pages = epubLoader.load(file)
        _uiState.update {
            it.copy(
                type = ReaderType.EPUB,
                pages = pages,
                currentPage = 0
            )
        }
    }

    fun nextPage(){
        _uiState.update {
            it.copy(
                currentPage = (it.currentPage + 1).coerceAtMost(it.pages.lastIndex)
            )
        }
    }

    fun prevPage(){
        _uiState.update {
            it.copy(
                currentPage = (it.currentPage - 1).coerceAtLeast(0)
            )
        }
    }

    fun toggleUI(){
        _uiState.update {
            it.copy(showUI = !it.showUI)
        }
    }
}

data class ReaderUiState(
    val type: ReaderType = ReaderType.TXT,
    val pages: List<String> = emptyList(),
    val currentPage: Int = 0,
    val showUI: Boolean = true
)
