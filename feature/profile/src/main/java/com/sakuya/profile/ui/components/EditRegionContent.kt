package com.sakuya.profile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakuya.profile.viewmodel.RegionItem
import com.sakuya.ui.theme.SakuyaInAndroidTheme


@Composable
fun EditRegionContent(
    regionList: List<RegionItem>,
    selectedCode: String,
    onSelect: (RegionItem) -> Unit
){
    LazyColumn(
        modifier = Modifier.fillMaxWidth()
    ) {
        items(
            items = regionList,
            key = { it.code }
        ){
            region ->
            RegionOptionRow(
                text = region.name,
                isSelected = selectedCode == region.code,
                onClick = { onSelect(region)}
            )
        }
    }
}

@Composable
fun RegionOptionRow(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
){
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = if(isSelected) FontWeight.Bold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface
        )
        if(isSelected){
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "已选中",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}


@Preview(showBackground = true)
@Composable
fun EditRegionPreview(){
    SakuyaInAndroidTheme(true) {
        EditRegionContent(
            listOf(RegionItem("1","123"),RegionItem("2","234")),
            "",
            onSelect = {},
        )
    }
}