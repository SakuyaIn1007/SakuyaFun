package com.sakuya.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** 无数字更新提示点；数量型未读仍应使用 Material Badge，避免混淆两种语义。 */
@Composable
fun UpdateDot(modifier:Modifier=Modifier){
    Box(modifier.size(7.dp).background(MaterialTheme.colorScheme.error,CircleShape).semantics{contentDescription="有新内容"})
}
