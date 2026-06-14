package com.sakuya.profileservices.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

sealed class AlbumTab(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Albums : AlbumTab("albums", "相册", Icons.AutoMirrored.Filled.List)
    object Photos : AlbumTab("photos", "照片", Icons.Default.Home)
    object Videos : AlbumTab("videos", "视频", Icons.Default.PlayArrow)
}

@Composable
fun AlbumMainScreen(
    onBack: () -> Unit = {}
) {
    val mockPictures = List(9) { index ->
        AlbumData(
            id = index.toString(),
            url = "https://picsum.photos/400?random=$index",
            isLock = index % 2 == 0,
            title = "精选照片 $index"
        )
    }

    var currentTab by remember { mutableStateOf<AlbumTab>(AlbumTab.Albums) }

    AlbumMainContent(
        pictures = mockPictures,
        currentTab = currentTab,
        onTabSelected = { currentTab = it },
        onBack = onBack
    )
}

@Composable
fun AlbumMainContent(
    pictures: List<AlbumData>,
    currentTab: AlbumTab,
    onTabSelected: (AlbumTab) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            AppSecondaryTopBar(
                title = "相册",
                onBack = onBack
            )
        },
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            AlbumBottomBar(
                currentTab = currentTab,
                onTabSelected = onTabSelected
            )
        }
    ) { innerPadding ->
        when (currentTab) {
            AlbumTab.Albums -> {
                AlbumsContent(
                    albums = pictures,
                    modifier = Modifier.padding(innerPadding),
                )
            }

            AlbumTab.Photos -> {
                PictureContent(listOf())
            }

            AlbumTab.Videos -> {
                VideoScreen()
            }
        }
    }
}

@Composable
fun AlbumBottomBar(
    currentTab: AlbumTab,
    onTabSelected: (AlbumTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        AlbumTab.Albums,
        AlbumTab.Photos,
        AlbumTab.Videos
    )

    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background
    ) {
        items.forEach { tab ->
            val selected = currentTab == tab
            NavigationBarItem(
                selected = false,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.title,
                        tint = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onBackground
                    )
                },
                label = {
                    Text(
                        text = tab.title,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onBackground
                    )
                }
            )
        }
    }
}

data class AlbumData(
    val id: String,
    val url: String,
    val isLock: Boolean,
    val title: String = ""
)

@Composable
fun AlbumsContent(
    albums: List<AlbumData>,
    modifier: Modifier = Modifier,
    onCreateAlbumClick: () -> Unit = {}
) {
    Column(
        modifier = modifier.background(MaterialTheme.colorScheme.background)
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
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

@Composable
fun CreateAlbumItem(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "新建相册",
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "新建相册",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

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
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Image(
                painter = rememberAsyncImagePainter(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(album.url)
                        .crossfade(true)
                        .build()
                ),
                contentDescription = album.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
        }

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
fun AlbumMainContentPreview() {
    val mockPictures = (0..5).map { index ->
        AlbumData(
            id = index.toString(),
            url = "https://picsum.photos/400?random=$index",
            isLock = index % 2 == 0,
            title = "照片 $index"
        )
    }
    SakuyaInAndroidTheme(true) {
        AlbumMainContent(
            pictures = mockPictures,
            currentTab = AlbumTab.Albums,
            onTabSelected = {},
            onBack = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AlbumBottomBarPreview() {
    SakuyaInAndroidTheme(darkTheme = true) {
        AlbumBottomBar(
            currentTab = AlbumTab.Albums,
            onTabSelected = {}
        )
    }
}
