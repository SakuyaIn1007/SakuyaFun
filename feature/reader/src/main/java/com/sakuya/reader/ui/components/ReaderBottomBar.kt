package com.sakuya.reader.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun ReaderBottomBar(
    progress: Float,
    fontSizeSp: Float,
    onFontSizeChanged: (Float) -> Unit = {}
) {
    ReaderBottomBarContent(
        progress = progress,
        fontSizeSp = fontSizeSp,
        onFontSizeChanged = onFontSizeChanged
    )
}

@Composable
fun ReaderBottomBarContent(
    progress: Float,
    fontSizeSp: Float,
    onFontSizeChanged: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(end = 8.dp)
            )
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ReaderBottomBarPreview() {
    ReaderBottomBarContent(
        progress = 0.42f,
        fontSizeSp = 18f,
        onFontSizeChanged = {}
    )
}
