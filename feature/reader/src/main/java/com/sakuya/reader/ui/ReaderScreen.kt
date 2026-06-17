package com.sakuya.reader.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sakuya.reader.model.ReaderType
import com.sakuya.reader.ui.components.ReaderBottomBar
import com.sakuya.reader.ui.components.ReaderTopBar
import com.sakuya.reader.ui.subpages.EpubReaderContent
import com.sakuya.reader.ui.subpages.TxtReaderContent
import com.sakuya.reader.viewmodel.ReaderUiState
import com.sakuya.reader.viewmodel.ReaderViewModel
import com.sakuya.ui.theme.SakuyaInAndroidTheme

@Composable
fun ReaderScreen(
    viewModel: ReaderViewModel
){
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ReaderContent(
        state = state,
        onNext = viewModel::nextPage,
        onPrev = viewModel::nextPage,
        onToggleUI = viewModel::toggleUI
    )
}

@Composable
fun ReaderContent(
    state: ReaderUiState,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onToggleUI: () -> Unit
){

    var showUI by remember { mutableStateOf(true) }
    Scaffold(
        topBar = {
            AnimatedVisibility(showUI) {
                ReaderTopBar()
            }
        },
        bottomBar = {
            AnimatedVisibility(showUI) {
                ReaderBottomBar()
            }
        }
    ){innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding)
                .clickable{
                    showUI = !showUI
                }
        ){
            when(state.type){
                ReaderType.TXT -> TxtReaderContent(
                    pages = state.pages,
                    currentPage = state.currentPage
                )
                ReaderType.EPUB -> EpubReaderContent(
                    pages = state.pages,
                    currentPage = state.currentPage
                )
            }


        }

    }
}

@Preview(showBackground = true)
@Composable
fun TxtReaderPreview(){
    val fakeState = ReaderUiState(
        type = ReaderType.TXT,
        pages = listOf(
            "第一页内容",
            "第二页内容",
            "第三页内容"
        ),
        currentPage = 0,
        showUI = true
    )
    SakuyaInAndroidTheme(true) {
        ReaderContent(
            state = fakeState,
            onNext = {},
            onPrev = {},
            onToggleUI = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun EpubReaderPreview(){
    val fakeState = ReaderUiState(
        type = ReaderType.EPUB,
        pages = listOf(
            "<h1>EPUB Page 1</h1>",
            "<h1>EPUB Page 2</h1>"
        ),
        currentPage = 0
    )
    SakuyaInAndroidTheme(true) {
        ReaderContent(
            state = fakeState,
            onNext = {},
            onPrev = {},
            onToggleUI = {}
        )
    }
}