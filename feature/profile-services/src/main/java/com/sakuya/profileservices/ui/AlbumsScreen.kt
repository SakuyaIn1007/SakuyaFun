package com.sakuya.profileservices.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.sakuya.designsystem.icon.SakuyaIcons
import com.sakuya.ui.component.AppSecondaryTopBar
import com.sakuya.ui.theme.SakuyaInAndroidTheme


sealed class AlbumScreen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Albums : AlbumScreen("albums", "相册", Icons.AutoMirrored.Filled.List)
    object Photos : AlbumScreen("photos", "照片", Icons.Default.Home)
    object Videos : AlbumScreen("videos", "视频", Icons.Default.PlayArrow)
}

@Composable
fun AlbumMainScreen(
    onBack: () -> Unit = {}
) {

    // 假数据用来预览
    val mockPictures = List(9) { index ->
        AlbumData(
            id = index.toString(),
            url = "https://picsum.photos/400?random=$index", // 随机图片 API 喵
            isLock = index % 2 == 0,
            title = "精选照片 $index"
        )
    }

    // 用一个局部状态记录当前停留在哪个页面，默认在“相册”页喵
    var currentScreen by remember { mutableStateOf<AlbumScreen>(AlbumScreen.Albums) }

    Scaffold(
        topBar = {
            AppSecondaryTopBar(
                title = "相册",
                onBack = onBack
            )
        },
        modifier = Modifier.fillMaxSize(),
        // ✨ 关键点：把我们写好的底部栏直接塞进脚手架的 bottomBar 肚子里喵！
        bottomBar = {
            AlbumBottomBar(
                currentScreen = currentScreen,
                onScreenSelected = { selected ->
                    currentScreen = selected // 点击时切换状态喵！
                }
            )
        }
    ) { innerPadding ->
        // 根据当前选中的状态，在剩下的屏幕空间里展示不同的纯 UI 内容喵！
        // 记得把 innerPadding 传给内部页面，防止内容被底部导航栏挡住喵！
        when (currentScreen) {
            AlbumScreen.Albums -> {
                AlbumsContent(
                    albums = mockPictures,
                    modifier = Modifier.padding(innerPadding),
                )
            }

            AlbumScreen.Photos -> {
                PictureContent(
                    listOf()
                )
            }

            AlbumScreen.Videos -> {
                // 视频列表或者播放器界面喵
                VideoScreen(

                )
            }
        }
    }
}

@Composable
fun AlbumBottomBar(
    currentScreen: AlbumScreen,
    onScreenSelected: (AlbumScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    // 准备好我们要显示的三个标签页列表喵
    val items = listOf(
        AlbumScreen.Albums,
        AlbumScreen.Photos,
        AlbumScreen.Videos
    )

    NavigationBar(
        modifier = modifier,
        // 可以利用主题色让背景看起来更柔和喵
        containerColor = MaterialTheme.colorScheme.background
    ) {
        items.forEach { screen ->
            val selected = currentScreen == screen
            NavigationBarItem(
                selected = false,
                // 点击的时候，把选中的屏幕丢给上一层处理
                onClick = { onScreenSelected(screen) },
                // 图标配置
                icon = {
                    Icon(
                        imageVector = screen.icon,
                        contentDescription = screen.title,
                        tint = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onBackground


                    )
                },
                // 文字配置
                label = {
                    Text(
                        text = screen.title,
                        style = MaterialTheme.typography.labelMedium,
                        color = if(selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onBackground
                    )
                }
            )
        }
    }
}

// 相册图片的数据模型喵
data class AlbumData(
    val id: String,
    val url: String,
    val isLock: Boolean,
    val title: String = ""
)


//相册内容
@Composable
fun AlbumsContent(
    albums: List<AlbumData>,
    modifier: Modifier = Modifier,
    onCreateAlbumClick: () -> Unit = {}
) {
    Column(
        modifier = modifier.background(MaterialTheme.colorScheme.background)
    ) {
        // 网格布局
        LazyVerticalGrid(
            columns = GridCells.Fixed(2), // 一行固定 3 个格子，咲夜觉得不合适可以随时改喵
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // 霸占第一个格子，用来渲染“新建相册”按钮
            item {
                CreateAlbumItem(
                    onClick = onCreateAlbumClick,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            items(
                items = albums,
                key = { it.id }
            ) { album ->
                AlbumItem(
                    album = album,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}


//新建相册
@Composable
fun CreateAlbumItem(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp) // 保持和 PictureItem 的垂直间距一致喵
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { onClick() },
            contentAlignment = Alignment.Center // 加号和文字居中
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // 居中的大加号图标
                Icon(
                    imageVector = Icons.Default.Add, // 使用系统的加号图标喵
                    contentDescription = "新建相册",
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // 提示小文字
                Text(
                    text = "新建相册",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 底部留出和 PictureItem 相同高度的空白占位，保证网格底部对齐线条不会乱喵！
        Spacer(modifier = Modifier.height(16.dp))
    }
}


@Composable
fun AlbumItem(
    album: AlbumData,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f) // 1f 代表 1:1 的正方形格子
                .clip(RoundedCornerShape(8.dp)) // 把圆角剪裁放在外层 Box，更统一喵
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Image(
                painter = rememberAsyncImagePainter(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(album.url) // 绑定实际图片喵
                        .crossfade(true)
                        .build()
                ),
                contentDescription = album.title,
                contentScale = ContentScale.Crop, // 让图片铺满整个正方形
                modifier = Modifier.matchParentSize() // matchParentSize
            )
        }

        // 相册的详细信息（是否被锁，和相册名称）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (album.isLock) {
                Icon(
                    painter = painterResource(SakuyaIcons.Favorites),
                    contentDescription = "status",
                    modifier = Modifier.size(16.dp),
                    tint = Color.Unspecified
                )
            }
            Text(
                text = "123456",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}


//预览
@Preview(showBackground = true)
@Composable
fun AlbumItemPreview() {
    SakuyaInAndroidTheme(true) {
        Box(modifier = Modifier
            .padding(16.dp)
            .size(120.dp)) {
            AlbumItem(
                album = AlbumData("1", "", true, "预览图片")
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AlbumMainPreview() {
    SakuyaInAndroidTheme(true) {
        AlbumMainScreen(

        )
    }
}

@Preview(showBackground = true)
@Composable
fun AlbumBottomBarPreview() {
    SakuyaInAndroidTheme(darkTheme = true) {
        AlbumBottomBar(
            currentScreen = AlbumScreen.Albums,
            onScreenSelected = {}
        )
    }
}
