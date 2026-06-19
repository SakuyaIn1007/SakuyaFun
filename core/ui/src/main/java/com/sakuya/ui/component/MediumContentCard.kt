package com.sakuya.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sakuya.designsystem.icon.SakuyaIcons
import com.sakuya.ui.theme.SakuyaInAndroidTheme

@Composable
fun MediumContentCard(
    title: String,
    progress: Float,
    fileType: String,
    modifier: Modifier = Modifier
) {
    val colors = listOf(Color(0xFF8EA7D8), Color(0xFF657EBA), Color(0xFF526BA8))
    val safeProgress = progress.coerceIn(0f, 1f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(min = 132.dp)
            .background(Color.Transparent),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .aspectRatio(0.75f)
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(4.dp),
                        ambientColor = Color.White.copy(alpha = 0.12f),
                        spotColor = Color.Black.copy(alpha = 0.42f)
                    )
                    .clip(RoundedCornerShape(4.dp))
                    .background(Brush.verticalGradient(colors))
            ) {
                CoverFrame(modifier = Modifier.matchParentSize())
                Text(
                    text = "-${fileType.uppercase()}-",
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 10.dp),
                    color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LinearProgressIndicator(
                progress = { safeProgress },
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = MaterialTheme.colorScheme.outline,
                trackColor = Color.White.copy(alpha = 0.12f)
            )
            Text(
                text = "${(safeProgress * 100).toInt()}%",
                color = MaterialTheme.colorScheme.outline,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp)
            ,
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(SakuyaIcons.Folder),
                contentDescription = "文件标",
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.width(20.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = title,
                color = MaterialTheme.colorScheme.outline,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}



@Composable
private fun CoverFrame(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = 1.4.dp.toPx()
        val inset = 12.dp.toPx()
        val corner = 16.dp.toPx()
        val lineColor = Color.White.copy(alpha = 0.28f)

        drawLine(
            color = Color.White.copy(alpha = 0.18f),
            start = Offset(12.dp.toPx(), 0f),
            end = Offset(12.dp.toPx(), size.height),
            strokeWidth = 1.2.dp.toPx()
        )
        drawLine(
            color = Color.White.copy(alpha = 0.14f),
            start = Offset(18.dp.toPx(), 0f),
            end = Offset(18.dp.toPx(), size.height),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(lineColor, Offset(inset, inset), Offset(size.width - inset, inset), stroke)
        drawLine(lineColor, Offset(inset, inset), Offset(inset, size.height - inset), stroke)
        drawLine(lineColor, Offset(size.width - inset, inset), Offset(size.width - inset, size.height - inset), stroke)
        drawLine(lineColor, Offset(inset, size.height - inset), Offset(inset + corner, size.height - inset), stroke)
        drawLine(lineColor, Offset(size.width - inset - corner, size.height - inset), Offset(size.width - inset, size.height - inset), stroke)
    }
}

@Preview(name = "Compact", showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun MediumContentCardCompactPreview() {
    SakuyaInAndroidTheme(true) {
        MediumContentCard(
            title = "3387",
            progress = 0.1f,
            fileType = "txt",
            modifier = Modifier
                .width(136.dp)
                .padding(12.dp)
        )
    }
}

@Preview(name = "Regular", showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun MediumContentCardRegularPreview() {
    SakuyaInAndroidTheme(true) {
        MediumContentCard(
            title = "3387",
            progress = 0.1f,
            fileType = "txt",
            modifier = Modifier
                .width(184.dp)
                .padding(16.dp)
        )
    }
}

@Preview(name = "Wide", showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun MediumContentCardWidePreview() {
    SakuyaInAndroidTheme(true) {
        MediumContentCard(
            title = "3387",
            progress = 0.1f,
            fileType = "txt",
            modifier = Modifier
                .width(260.dp)
                .padding(20.dp)
        )
    }
}
