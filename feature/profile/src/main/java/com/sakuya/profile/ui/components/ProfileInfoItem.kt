package com.sakuya.profile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ProfileInfoItem(
    title: String,
    modifier: Modifier = Modifier,
    showArrow: Boolean = true,
    onClick: ()-> Unit = {},
//  显示对应的个人信息
    trailingContent: @Composable RowScope.() ->Unit = {}
){
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(MaterialTheme.colorScheme.surface)
            .clickable{onClick()}
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ){
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = title
            , style = MaterialTheme.typography.titleMedium
            , color = MaterialTheme.colorScheme.onBackground
            , modifier = Modifier.weight(1f))
        trailingContent()
        if(showArrow) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "箭头",
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}
