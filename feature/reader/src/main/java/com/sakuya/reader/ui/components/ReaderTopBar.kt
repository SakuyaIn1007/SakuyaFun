package com.sakuya.reader.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sakuya.ui.theme.SakuyaInAndroidTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderTopBar(){
    //ReaderTopBarContent()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderTopBarContent(
    title: String,
    onBack: () -> Unit,
    showNavigationIcon: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {}
){
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
        } ,
        navigationIcon = {
            if (showNavigationIcon) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        modifier = Modifier.size(24.dp),
                        contentDescription = "back"
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        ),
        actions = actions
    )
}



@Preview(showBackground = true)
@Composable
fun ReaderTopBarPreview(){
    SakuyaInAndroidTheme(true) {
        ReaderTopBarContent(
            title = "123",
            onBack = {},
            showNavigationIcon = true
        )
    }
}