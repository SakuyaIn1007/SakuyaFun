package com.sakuya.profileservices.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.sakuya.common.utils.DateUtils
import com.sakuya.profileservices.data.remote.FavouritesItemData
import com.sakuya.ui.component.AppSecondaryTopBar
import com.sakuya.ui.theme.SakuyaInAndroidTheme
import java.util.Date

@Composable
fun FavouritesScreen(
    favourites: List<FavouritesItemData>,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onClick: () -> Unit = {}
) {
    FavouritesContent(
        favourites = favourites,
        modifier = modifier,
        onBack = onBack,
        onClick = onClick
    )
}

@Composable
fun FavouritesContent(
    favourites: List<FavouritesItemData>,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onClick: () -> Unit = {},
){
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AppSecondaryTopBar(
            title = "收藏",
            onBack = onBack
        )
        LazyColumn(

        ) {
            favourites.forEach { favouritesItemData ->
                item{
                    FavouritesItem(
                        favoritesItemData = favouritesItemData,
                        onClick = onClick
                    )
                }
            }
        }
    }
}

@Composable
fun FavouritesItem(
    favoritesItemData: FavouritesItemData,
    onClick: () -> Unit = {}
){
    OutlinedCard(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults
            .elevatedCardColors
                (containerColor = MaterialTheme.colorScheme.inverseOnSurface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Column(
            modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

//          收藏标题+图片内容
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = favoritesItemData.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .weight(1f)
                )
                Image(
                    painter = rememberAsyncImagePainter(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(favoritesItemData.imageUrl)
                            .crossfade(true)
                            .build()
                    ),
                    contentDescription = "图",
                    modifier = Modifier
                            .size(68.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                )

            }
//            来源 + 日期
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
//                来源
                Text(
                    text = favoritesItemData.resources,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier
                )
//                日期
                Text(
                    text = DateUtils.format(Date(),"yyyy年MM月dd日"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier
                        .padding(end = 2.dp)
                )
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun FavouritesItemPreview(){
    SakuyaInAndroidTheme(true) {
        FavouritesItem(
                FavouritesItemData(
                title =  "111",
                imageUrl = "https://i0.hdslb.com/bfs/archive/f225baea1791f6cc35addffab219515aee4639a7.jpg",
                resources = "123",
                date = Date()
                )
            )
    }
}
@Preview(showBackground = true)
@Composable
fun FavouritesPreview(){
    // 模拟数据
    //val fakeFavourites =
    SakuyaInAndroidTheme(true) {
        FavouritesContent(
            listOf(
                FavouritesItemData(
                title =  "111",
                imageUrl = "https://i0.hdslb.com/bfs/archive/f225baea1791f6cc35addffab219515aee4639a7.jpg",
                resources = "123",
                date = Date())
            )
        )
    }
}
