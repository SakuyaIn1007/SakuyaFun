package com.sakuya.catalog.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sakuya.catalog.model.NovelRelease
import com.sakuya.catalog.model.NovelReleaseDate
import com.sakuya.catalog.model.NovelReleaseDayGroup
import com.sakuya.catalog.viewmodel.ScheduleUiState
import java.util.Calendar
import java.util.GregorianCalendar

/**
 * NovelPublishContent.kt
 * 职责说明：渲染已公布轻小说更新时间表，包括更新推荐 Banner 与按日期聚合的完整更新列表。
 * 执行流程：接收 ViewModel 已排序的日期组 -> 先展示推荐横滑列表 -> 再逐组展示有小说更新的日期与条目。
 */
@Composable
internal fun CatalogScheduleContent(
    uiState: ScheduleUiState,
    onBookClick: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "recommend_title") {
            ScheduleSectionTitle("更新推荐")
        }

        item(key = "recommend_banners") {
            RecommendedReleaseBanners(
                releases = uiState.recommendedReleases,
                onBookClick = onBookClick,
            )
        }

        item(key = "updates_title") {
            ScheduleSectionTitle("更新小说")
        }

        /** releaseGroups 只由有更新的小说分组生成，因此不会渲染任何空日期。 */
        items(uiState.releaseGroups, key = { it.date.toStableKey() }) { group ->
            ReleaseDaySection(group = group, onBookClick = onBookClick)
        }
    }
}

@Composable
private fun ScheduleSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(horizontal = 16.dp),
    )
}

@Composable
private fun RecommendedReleaseBanners(
    releases: List<NovelRelease>,
    onBookClick: (String) -> Unit,
) {
    if (releases.isEmpty()) {
        Text(
            text = "暂无更新推荐",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        return
    }

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(releases, key = { it.novel.id }) { release ->
            ReleaseBanner(release = release, onClick = { onBookClick(release.novel.id) })
        }
    }
}

@Composable
private fun ReleaseBanner(
    release: NovelRelease,
    onClick: () -> Unit,
) {
    val colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
    )
    Column(
        modifier = Modifier
            .width(260.dp)
            .height(136.dp)
            .clip(MaterialTheme.shapes.large)
            .background(Brush.linearGradient(colors))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = release.releaseDate.displayLabel(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f),
        )
        Column {
            Text(
                text = release.novel.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = release.volumeName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ReleaseDaySection(
    group: NovelReleaseDayGroup,
    onBookClick: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = group.date.displayLabel(),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        group.releases.forEach { release ->
            ReleaseNovelItem(release = release, onClick = { onBookClick(release.novel.id) })
        }
    }
}

@Composable
private fun ReleaseNovelItem(
    release: NovelRelease,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(width = 48.dp, height = 64.dp)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = release.novel.title.take(1),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = release.novel.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = release.volumeName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 2.dp),
            )
            Text(
                text = release.novel.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

/** 同年省略年份，跨年日期保留年份；星期根据实际日历日期计算。 */
private fun NovelReleaseDate.displayLabel(currentYear: Int = Calendar.getInstance().get(Calendar.YEAR)): String {
    val weekDay = GregorianCalendar(year, month - 1, day).get(Calendar.DAY_OF_WEEK).toChineseWeekDay()
    val dateText = if (year == currentYear) "${month}月${day}日" else "${year}年${month}月${day}日"
    return "$dateText $weekDay"
}

private fun Int.toChineseWeekDay(): String = when (this) {
    Calendar.MONDAY -> "周一"
    Calendar.TUESDAY -> "周二"
    Calendar.WEDNESDAY -> "周三"
    Calendar.THURSDAY -> "周四"
    Calendar.FRIDAY -> "周五"
    Calendar.SATURDAY -> "周六"
    else -> "周日"
}

private fun NovelReleaseDate.toStableKey(): String = "$year-$month-$day"
