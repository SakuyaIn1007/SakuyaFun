package com.sakuya.ui.component

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

@Composable
fun ShrinkText(
    text: String,
    style: TextStyle,
    weight: FontWeight,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier) {
        text.forEachIndexed { index, char ->

            val progress = index / (text.length - 1f)
            val scale = 1f - 0.3f * progress // 左大右小

            Text(
                text = char.toString(),
                style = style,
                fontWeight = weight,
                modifier = Modifier.graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
            )
        }
    }
}