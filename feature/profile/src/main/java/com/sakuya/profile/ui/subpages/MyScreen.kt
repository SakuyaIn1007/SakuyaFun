package com.sakuya.profile.ui.subpages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.sakuya.profile.model.PrivacySettings
import com.sakuya.profile.model.UserProfile
import com.sakuya.profile.ProfileAction
import com.sakuya.profile.ui.components.ProfileInfoItem
import com.sakuya.profile.viewmodel.ProfileMeViewModel
import com.sakuya.ui.component.AppSecondaryTopBar
import com.sakuya.ui.theme.SakuyaInAndroidTheme

@Composable
fun MyScreen(
    viewModel: ProfileMeViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
    onAction: (ProfileAction) -> Unit = {},
    onBack:()->Unit = {},
) {
    val profile by viewModel.userProfile.collectAsState()
    MyContent(
        profile,
        modifier,
        onAction = onAction,
        onBack = onBack)
}

@Composable
fun MyContent(
    profile: UserProfile,
    modifier: Modifier,
    onAction: (ProfileAction) -> Unit = {},
    onBack:()->Unit = {},){
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
        ,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AppSecondaryTopBar(
            title = "个人资料",
            onBack = onBack
        )
        ProfileInfoItem(title = "头像"){
            AsyncImage(
                model = profile.avatarUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
        }
        ProfileOutline(12.dp)
        ProfileInfoItem(
            title = "姓名",
            onClick = {onAction( ProfileAction.OnNameClick )}
        ){
            Text(text = profile.nickname
                , style = MaterialTheme.typography.titleMedium
                , color = MaterialTheme.colorScheme.onTertiary
            )
        }
        ProfileOutline(12.dp)
        ProfileInfoItem(title = "性别"){
            Text(text = profile.gender?.displayName ?: "未知"
                , style = MaterialTheme.typography.titleMedium
                , color = MaterialTheme.colorScheme.onTertiary
            )
        }
        ProfileOutline(12.dp)
        ProfileInfoItem(title = "地区"){
            Text(text = profile.regionCode?: "未知"
                , style = MaterialTheme.typography.titleMedium
                , color = MaterialTheme.colorScheme.onTertiary
            )
        }
        ProfileOutline(12.dp)
        ProfileInfoItem(title = "电话号码"){
            Text(text = profile.phoneNumber?: ""
                , style = MaterialTheme.typography.titleMedium
                , color = MaterialTheme.colorScheme.onTertiary
            )
        }
        ProfileOutline(12.dp)
        ProfileInfoItem(title = "ID"){
            Text(text = profile.userId
                , style = MaterialTheme.typography.titleMedium
                , color = MaterialTheme.colorScheme.onTertiary
            )
        }
        ProfileOutline(12.dp)
        ProfileInfoItem(title = "戳一下"){
            Text(text = profile.signature?: ""
                , style = MaterialTheme.typography.titleMedium
                , color = MaterialTheme.colorScheme.onTertiary
            )
        }
        ProfileOutline(12.dp)
        ProfileInfoItem(title = "个性签名"){
            Text(text = profile.signature?: ""
                , style = MaterialTheme.typography.titleMedium
                , color = MaterialTheme.colorScheme.onTertiary
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        ProfileInfoItem(title = "电话铃声")
    }

}


@Composable
fun ProfileOutline(dp: Dp){
    HorizontalDivider(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = dp)
        ,
        thickness = 0.5.dp, // 0.5dp 是最“极细丝滑线条”厚度
        color = MaterialTheme.colorScheme.outlineVariant
    )
}
@Preview(showBackground = true)
@Composable
fun MyScreenPreview() {
    // 模拟数据
    val fakeProfile = UserProfile(
        userId = "0",
        avatarUrl = "https://i0.hdslb.com/bfs/archive/f225baea1791f6cc35addffab219515aee4639a7.jpg",
        nickname = "智乃",
        signature = "这是签名",
        gender = null,
        birthday = null,
        regionCode = null,
        phoneNumber = null,
        email = null,
        privacySettings = PrivacySettings()
    )
    SakuyaInAndroidTheme(true) {
            MyContent(
            fakeProfile,
            modifier = Modifier,
            onAction = {}
        )
    }

}
