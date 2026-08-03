package com.sakuya.reader.ui.subpages

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakuya.ui.theme.SakuyaInAndroidTheme
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlin.math.roundToInt

@Composable
fun TxtReaderContent(
    fullText: String,
    fontSizeSp: Float,
    initialProgress: Float,
    onProgress: (Float) -> Unit
) {
    if (fullText.isEmpty()) return

    val textStyle = remember(fontSizeSp) {
        TextStyle(
            fontSize = fontSizeSp.sp,
            lineHeight = (fontSizeSp * 1.6f).sp
        )
    }

    val scrollState = rememberScrollState()
    val restoredProgress = remember(fullText) {
        initialProgress.coerceIn(0f, 1f)
    }

    LaunchedEffect(fullText) {
        val max = snapshotFlow { scrollState.maxValue }
            .filter { it > 0 }
            .first()
        scrollState.scrollTo((max * restoredProgress).roundToInt())
    }

    LaunchedEffect(scrollState) {
        snapshotFlow { scrollState.value }
            .collect { offset ->
                val max = scrollState.maxValue
                if (max > 0) {
                    onProgress(offset.toFloat() / max.toFloat())
                }
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = fullText,
            style = textStyle.copy(color = MaterialTheme.colorScheme.onBackground),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TxtReaderPreview() {
    val sample = buildString {
        for (i in 1..100) {
            appendLine("第 $i 行：这是测试文本内容，用于验证滚动阅读模式加进度条。")
        }
    }
    SakuyaInAndroidTheme(true) {
        TxtReaderContent(
            fullText = sample,
            fontSizeSp = 18f,
            initialProgress = 0f,
            onProgress = {}
        )
    }
}
