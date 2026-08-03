package com.sakuya.profile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sakuya.profile.viewmodel.ProfileViewModel

@Composable
fun ProfilePrivacyScreen(
    viewModel: ProfileViewModel,
    onBack: () -> Unit
) {
    val profile by viewModel.userProfile.collectAsState()
    var privacy by remember { mutableStateOf(profile.privacySettings) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 顶部返回按钮
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            TextButton(onClick = onBack) { Text("返回") }
            TextButton(onClick = {
                val updatedProfile = profile.copy(privacySettings = privacy)
                viewModel.updateProfile(updatedProfile, onSuccess = onBack)
            }) { Text("保存") }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 设置开关
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("允许陌生人添加我为好友")
                Switch(
                    checked = privacy.canBeAddedByStrangers,
                    onCheckedChange = { privacy = privacy.copy(canBeAddedByStrangers = it) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("陌生人可查看我的资料")
                Switch(
                    checked = privacy.showProfileToStrangers,
                    onCheckedChange = { privacy = privacy.copy(showProfileToStrangers = it) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("屏蔽陌生人消息")
                Switch(
                    checked = privacy.muteMessagesFromUnknown,
                    onCheckedChange = { privacy = privacy.copy(muteMessagesFromUnknown = it) }
                )
            }
        }
    }
}
