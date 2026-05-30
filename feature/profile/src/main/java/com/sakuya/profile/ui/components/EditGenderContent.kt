package com.sakuya.profile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakuya.model.extentions.Gender
import com.sakuya.ui.theme.SakuyaInAndroidTheme

@Composable
fun EditGenderContent(
    selected: Gender,
    onSelect: (Gender) -> Unit
){
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Gender.entries.forEach { gender ->
                if(gender != Gender.UNKNOWN) {
                    GenderOptionRow(
                        text = gender.displayName, // 拿枚举里的中文名显示喵
                        isSelected = selected == gender,
                        onClick = { onSelect(gender) } // 点了就传整个枚举对象出去喵
                    )
                }
            }
        }
}

@Composable
private fun GenderOptionRow(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp), // 稍微加大了一点内边距，点击区域更舒服喵
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween // 让文字在左边，勾勾在右边喵
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            // 选中的时候字体加粗，颜色变成主题色喵
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface
        )

        // 只有选中的时候才显示右边的小勾勾喵
        if (isSelected) {
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
fun EditGenderPreview(){
    SakuyaInAndroidTheme(true) {
        EditGenderContent(selected = Gender.MALE,
            onSelect = {})
    }
}