package com.sakuya.profile.ui

import android.util.Log
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.sakuya.designsystem.icon.SakuyaIcons
import com.sakuya.library.ui.LibraryScreen
import com.sakuya.model.feed.DynamicPost
import com.sakuya.profile.model.PrivacySettings
import com.sakuya.profile.model.UserProfile
import com.sakuya.profile.viewmodel.ProfileAction
import com.sakuya.ui.component.PrimaryTabRow
import com.sakuya.ui.component.DynamicPostCard
import com.sakuya.ui.theme.SakuyaInAndroidTheme
import com.sakuya.model.notification.UpdateBadgeState
import com.sakuya.ui.component.UpdateDot
import androidx.compose.material3.BadgedBox

@Composable
fun ProfileScreen(
    profile: UserProfile,
    modifier: Modifier = Modifier,
    onAction: (ProfileAction) -> Unit = {},
    onOpenReader: (bookId: String, filePath: String) -> Unit = { _, _ -> },
    onOpenDynamic: () -> Unit = {},
    onOpenFollowing: () -> Unit = {},
    onOpenFollowers: () -> Unit = {},
    onOpenFavourites: () -> Unit = {},
    onOpenHistory: () -> Unit = {},
    updateState:UpdateBadgeState=UpdateBadgeState(),
    onDynamicViewed:()->Unit={},
) {
    ProfileContent(
        profile,
        modifier,
        onAction = onAction,
        onOpenReader = onOpenReader,
        onOpenDynamic = onOpenDynamic,
        onOpenFollowing = onOpenFollowing,
        onOpenFollowers = onOpenFollowers,
        onOpenFavourites = onOpenFavourites,
        onOpenHistory = onOpenHistory,
        updateState=updateState,
        onDynamicViewed=onDynamicViewed,
    )
}


@Composable
fun ProfileContent(
    profile: UserProfile,
    modifier: Modifier,
    onAction: (ProfileAction) -> Unit = {},
    onOpenReader: (bookId: String, filePath: String) -> Unit = { _, _ -> },
    onOpenDynamic: () -> Unit = {},
    onOpenFollowing: () -> Unit = {},
    onOpenFollowers: () -> Unit = {},
    onOpenFavourites: () -> Unit = {},
    onOpenHistory: () -> Unit = {},
    updateState:UpdateBadgeState=UpdateBadgeState(),
    onDynamicViewed:()->Unit={},
) {
    var selectedSection by remember { mutableIntStateOf(0) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ProfileSectionBar(selectedSection, { selectedSection = it;if(it==1)onDynamicViewed() },updateState.showProfileDynamic) {
            onAction(ProfileAction.OnSettingsClick)
        }
        ProfileHeader(
            profile = profile,
            onClick = { onAction(ProfileAction.OnMeClick) },
            onOpenFollowing = onOpenFollowing,
            onOpenFollowers = onOpenFollowers,
            onOpenFavourites = onOpenFavourites,
            onOpenHistory = onOpenHistory,
            updateState=updateState,
        )
        Spacer(Modifier.height(12.dp))
        Box(Modifier.weight(1f)) {
            if (selectedSection == 0) {
                LibraryScreen(onOpenReader = onOpenReader)
            } else {
                ProfileDynamicContent(profile, onOpenDynamic)
            }
        }
    }
}

@Composable
private fun ProfileDynamicContent(profile: UserProfile, onOpenDynamic: () -> Unit) {
    val posts = listOf(
        DynamicPost(
            id = "my-post-1",
            authorName = profile.nickname,
            authorInitial = profile.nickname.take(1).ifBlank { "我" },
            authorColor = 0xFF5D8C77,
            title = "我的周末阅读记录",
            content = "重新翻开搁置已久的小说，读到熟悉段落时还是会被其中的细节打动。",
            imageColors = listOf(0xFF8799B0, 0xFFC3947A, 0xFF749B90),
            tags = listOf("#阅读记录", "#周末"),
            commentCount = 6,
            likeCount = 23,
            userId = "current-user",
            isMine = true,
        ),
        DynamicPost(
            id = "my-post-2",
            authorName = profile.nickname,
            authorInitial = profile.nickname.take(1).ifBlank { "我" },
            authorColor = 0xFF5D8C77,
            title = "写在书页边的句子",
            content = "有些句子并不会立刻懂得，但在某个平常的夜晚又会突然想起。",
            tags = listOf("#摘抄", "#随想"),
            commentCount = 2,
            likeCount = 11,
            userId = "current-user",
            isMine = true,
        ),
    )
    LazyColumn(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        items(posts, key = DynamicPost::id) { post ->
            DynamicPostCard(post = post, onClick = onOpenDynamic)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
        }
    }
}

@Composable
private fun ProfileSectionBar(
    selectedSection: Int,
    onSectionSelected: (Int) -> Unit,
    showDynamicUpdate:Boolean,
    onSettingsClick: () -> Unit
) {
    PrimaryTabRow(
        tabs = listOf("书架", "动态"),
        selectedIndex = selectedSection,
        onTabSelected = onSectionSelected,
        badgeIndices=if(showDynamicUpdate)setOf(1)else emptySet(),
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp)
    ) {
        IconButton(onClick = onSettingsClick) {
            Icon(painterResource(SakuyaIcons.Settings), "设置", Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ProfileHeader(
    profile: UserProfile,
    onClick: () -> Unit = {},
    onOpenFollowing: () -> Unit = {},
    onOpenFollowers: () -> Unit = {},
    onOpenFavourites: () -> Unit = {},
    onOpenHistory: () -> Unit = {},
    updateState:UpdateBadgeState=UpdateBadgeState(),
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                    Log.d("ProfileHeader", "clicked")
                    onClick()
                }
                .padding(top = 24.dp, bottom = 24.dp)
        ) {
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
                size(72.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            }
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .height(72.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = profile.nickname,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = profile.signature?.takeIf { it.isNotBlank() } ?: "点击完善个人资料",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                maxLines = 1
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        MiniQrCode(
            modifier = Modifier.size(22.dp),
            color = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.width(6.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "箭头",
            tint = MaterialTheme.colorScheme.outline
        )
        }
        ProfileStats(profile,onOpenFollowing,onOpenFollowers,onOpenFavourites,onOpenHistory,updateState)
    }
}

@Composable
private fun ProfileStats(
    profile:UserProfile,
    onOpenFollowing: () -> Unit,
    onOpenFollowers: () -> Unit,
    onOpenFavourites: () -> Unit,
    onOpenHistory: () -> Unit,
    updateState:UpdateBadgeState,
) {
    val stats = listOf(
        Triple(profile.followingCount.toString(), "关注", onOpenFollowing),
        Triple(profile.followerCount.toString(), "粉丝", onOpenFollowers),
        Triple("0", "收藏", onOpenFavourites),
        Triple("0", "历史浏览", onOpenHistory),
    )
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 20.dp),
    ) {
        stats.forEach { (count, label, onClick) ->
            Column(
                modifier = Modifier.weight(1f).clickable(onClick = onClick),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(count, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
                Spacer(Modifier.height(1.dp))
                BadgedBox(badge={if((label=="关注"&&updateState.showFollowing)||(label=="粉丝"&&updateState.showFollowers))UpdateDot()}){
                    Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}

@Composable
private fun MiniQrCode(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.outline
) {
    Canvas(modifier = modifier) {
        val cell = size.minDimension / 5f
        val points = listOf(
            0 to 0, 1 to 0, 0 to 1,
            4 to 0, 3 to 0, 4 to 1,
            0 to 4, 0 to 3, 1 to 4,
            2 to 2, 3 to 3, 4 to 4, 2 to 4
        )
        points.forEach { (x, y) ->
            drawRect(
                color = color,
                topLeft = androidx.compose.ui.geometry.Offset(x * cell, y * cell),
                size = androidx.compose.ui.geometry.Size(cell * 0.78f, cell * 0.78f)
            )
        }
    }
}

@Composable
fun ProfileFunctionItem(
    title: String,
    @DrawableRes iconRes: Int,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onClick() }
            .padding(vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(12.dp))
        Icon(
            painter = painterResource(iconRes),
            contentDescription = title,
            modifier = Modifier.size(24.dp),
            tint = Color.Unspecified
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "箭头",
            tint = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
fun ProfileOutline() {
    HorizontalDivider(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 50.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
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
