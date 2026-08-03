package com.sakuya.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakuya.ui.theme.SakuyaInAndroidTheme

@Composable
fun GlobalSearchHeader(
    query: String = "",
    onQueryChanged: (String) -> Unit = {},
    onClearQuery: () -> Unit = {},
    onSearchClick: (() -> Unit)? = null,
    onFeatureClick: () -> Unit = {},
    showFeature: Boolean = true,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface
) {
    var isFocused by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(containerColor)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .then(
                    if (onSearchClick != null) {
                        Modifier.clickable { onSearchClick() }
                    } else {
                        Modifier
                    }
                )
                .then(
                    if (isFocused)
                        Modifier.shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(28.dp),
                            ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        )
                    else Modifier
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.background,
                                MaterialTheme.colorScheme.background.copy(alpha = 0.9f)
                            )
                        ),
                        shape = RoundedCornerShape(28.dp)
                    )
                    .border(
                        width = if (isFocused) 1.5.dp else 0.5.dp,
                        brush = if (isFocused)
                            Brush.horizontalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            )
                        else SolidColor(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(28.dp)
                    )
            ) {
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChanged,
                    singleLine = true,
                    readOnly = onSearchClick != null,
                    textStyle = TextStyle(
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary
                        )
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isFocused = it.isFocused }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) { innerTextField ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = if (isFocused)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.outline
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Box(modifier = Modifier.weight(1f)) {
                            if (query.isEmpty()) {
                                Text(
                                    text = "搜索你喜欢的内容...",
                                    style = TextStyle(
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                                    )
                                )
                            }
                            innerTextField()
                        }

                        AnimatedVisibility(
                            visible = query.isNotEmpty(),
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.outlineVariant)
                                    .clickable { onClearQuery() },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "✕",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showFeature) {
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            modifier = Modifier
                .size(44.dp)
                .clickable { onFeatureClick() },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "排行榜",
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "排行榜",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GlobalSearchHeaderPreview() {
    SakuyaInAndroidTheme(true) {
        GlobalSearchHeader()
    }
}
