package com.sakuya.profile.ui

import android.util.Log
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.sakuya.designsystem.icon.SakuyaIcons
import com.sakuya.profile.model.PrivacySettings
import com.sakuya.profile.model.UserProfile
import com.sakuya.profile.ProfileAction
import com.sakuya.profile.viewmodel.ProfileViewModel
import com.sakuya.ui.theme.SakuyaInAndroidTheme

@Composable
fun ProfileScreen(
    profile: UserProfile,
    modifier: Modifier = Modifier,
    onAction: (ProfileAction) -> Unit = {},
) {

    ProfileContent(
        profile,
        modifier,
        onAction = onAction
    )
}


@Composable
fun ProfileContent(
    profile: UserProfile,
    modifier: Modifier,
    onAction: (ProfileAction) -> Unit = {},
){

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        ProfileHeader(
            profile,
            onClick = {
            onAction(ProfileAction.OnMeClick)
        }
        )
        Spacer(modifier = Modifier.height(6.dp))
        ProfileFunctionItem(
            title = "消息",
            iconRes = SakuyaIcons.Conversation,
            onClick = { onAction(ProfileAction.OnConversationClick) }
        )
        ProfileOutline()
        ProfileFunctionItem(
            title = "好友",
            iconRes = SakuyaIcons.Friends,
            onClick = { onAction(ProfileAction.OnFriendsClick) }
        )
        Spacer(modifier = Modifier.height(6.dp))
        ProfileFunctionItem(
            title = "钱包",
            iconRes = SakuyaIcons.Wallet,
            onClick = { onAction(ProfileAction.OnWalletClick) }
        )
        Spacer(modifier = Modifier.height(6.dp))
        ProfileFunctionItem(
            title = "收藏",
            iconRes = SakuyaIcons.Favorites,
            onClick = { onAction(ProfileAction.OnFavouritesClick) }
            )
        ProfileOutline()
        ProfileFunctionItem(
            title = "相册",
            iconRes = SakuyaIcons.Pictures,
            onClick = { onAction(ProfileAction.OnAlbumsClick) }
        )
        ProfileOutline()
        ProfileFunctionItem(
            title = "卡包",
            iconRes = SakuyaIcons.Cards,
            onClick = { onAction(ProfileAction.OnCardsClick) }
        )
        Spacer(modifier = Modifier.height(6.dp))
        ProfileFunctionItem(
            title = "设置",
            iconRes = SakuyaIcons.Settings,
            onClick = { onAction(ProfileAction.OnSettingsClick) }
            )

    }
}

@Composable
fun ProfileHeader(
    profile: UserProfile
    , onClick:() -> Unit = {}
){
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.inverseOnSurface)
            .clickable{
                Log.d("ProfileHeader", "clicked")
                onClick()
            }
            .padding( top = 60.dp, bottom = 56.dp)
    ){
        Spacer(modifier = Modifier.width(16.dp))
        Image(
            painter = rememberAsyncImagePainter(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(profile.avatarUrl)
                    .crossfade(true)
                    .build()
            ),
            contentDescription = "头像",
            modifier = Modifier.run {
                size(68.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
            }
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .height(68.dp)
            ,
            verticalArrangement = Arrangement.SpaceBetween
        ){
            Text(text = profile.nickname
                , style = MaterialTheme.typography.titleLarge
                , color = MaterialTheme.colorScheme.onBackground
                , modifier = Modifier.padding(top = 2.dp)
            )
            Text(text = profile.signature ?: "这个人是条懒狗，什么都没有留下"
                , style = MaterialTheme.typography.bodySmall
                , color = MaterialTheme.colorScheme.outline
                , modifier = Modifier.padding(bottom = 6.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "箭头",
            tint = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
fun ProfileFunctionItem(title: String,
                        @DrawableRes iconRes: Int,
                        onClick: ()-> Unit = {}){
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.inverseOnSurface)
            .clickable{onClick()}
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ){
        Spacer(modifier = Modifier.width(12.dp))
        Icon(
            painter = painterResource(iconRes),
            contentDescription = title,
            modifier = Modifier.size(24.dp),
            tint = Color.Unspecified
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = title
            , style = MaterialTheme.typography.titleMedium
            , color = MaterialTheme.colorScheme.onBackground
            , modifier = Modifier.weight(1f))
        Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "箭头",
            tint = MaterialTheme.colorScheme.outline
            )
    }
}

@Composable
fun ProfileOutline(){
    HorizontalDivider(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 50.dp)
        ,
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant
    )
}
@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
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
        ProfileContent(
            fakeProfile,
            modifier = Modifier,
            onAction = {}
        )
    }

}
