package com.sakuya.ui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun Outline(dp: Dp){
    HorizontalDivider(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = dp)
        ,
        thickness = 0.5.dp, // 0.5dp 是最“极细丝滑线条”厚度
        color = MaterialTheme.colorScheme.outlineVariant
    )
}