package com.sakuya.profile.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.sakuya.profile.data.repository.UserRepository
import com.sakuya.profile.ui.components.EditRegionContent
import com.sakuya.profile.viewmodel.ProfileViewModel
import com.sakuya.profile.viewmodel.RegionItem
import com.sakuya.profile.viewmodel.UploadState
import com.sakuya.ui.component.AppSecondaryTopBar
import com.sakuya.ui.component.Outline
import com.sakuya.ui.theme.SakuyaInAndroidTheme

@Composable
fun ProfileEditScreen(
    title : String,
    onClick : () -> Unit = {},
    onSave : () -> Unit = {},
    onBack : () -> Unit,
    showSave: Boolean = false,
    saveEnabled: Boolean = true,
    content: @Composable () -> Unit = {},
) {


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AppSecondaryTopBar(
            title = title,
            onBack = onBack,
            actions = {
                if (showSave) {
                    TextButton(
                        onClick = onSave,
                        enabled = saveEnabled,
                    ) {
                        Text(text = "保存")
                    }
                }
            }
        )
        Column(
            modifier = Modifier.padding(vertical = 16.dp)
        ) {
            content()
        }
    }
}


//姓名
@Preview(showBackground = true)
@Composable
fun EditNamePreview(){
    SakuyaInAndroidTheme(true) {
        ProfileEditScreen(
            title = "更改名字",
            onBack = {},
            onSave = {
//
            }
        ){
            TextField(
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.background,
                    unfocusedContainerColor = MaterialTheme.colorScheme.background,
                    disabledContainerColor = MaterialTheme.colorScheme.background,
                    errorContainerColor = MaterialTheme.colorScheme.background
                ),
                value = "inputName",
                onValueChange = {},
                modifier = Modifier
                    .fillMaxWidth()
            )
        }
    }
}

//个性签名
@Preview(showBackground = true)
@Composable
fun EditSignaturePreview(){
    SakuyaInAndroidTheme(true) {
        ProfileEditScreen(
            title = "个性签名",
            onBack = {},
            onSave = {
//
            }
        ){
            TextField(
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.background,
                    unfocusedContainerColor = MaterialTheme.colorScheme.background,
                    disabledContainerColor = MaterialTheme.colorScheme.background,
                    errorContainerColor = MaterialTheme.colorScheme.background
                ),
                value = "inputName",
                onValueChange = {},
                modifier = Modifier
                    .fillMaxWidth()
            )
        }
    }
}


//头像
@Preview(showBackground = true)
@Composable
fun ProfileAvatarPreview(){
    SakuyaInAndroidTheme(true) {
        ProfileEditScreen(
            title = "上传头像",
            onClick = {

            },
            onBack = {}
        ){
            AvatarEditContent(
            currentAvatarUrl = "",
            onImageSelected = {
            },

        )
        }
    }

}


//地区
@Preview(showBackground = true)
@Composable
fun ProfileRegionPreview(){
    SakuyaInAndroidTheme(true) {
        ProfileEditScreen(
            title = "个性签名",
            onBack = {},
            onSave = {
//
            }
        ){
            //EditRegionContent()
        }
    }
}

//性别

//电话号
