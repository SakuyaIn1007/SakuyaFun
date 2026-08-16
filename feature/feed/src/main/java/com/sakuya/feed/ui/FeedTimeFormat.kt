package com.sakuya.feed.ui

import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * FeedTimeFormat.kt
 * 职责说明：将后端 ISO 时间统一转换为动态详情页可读的相对发布时间。
 * 执行流程：优先解析 Instant -> 根据与当前时间的间隔输出刚刚、分钟前、小时前或日期；旧格式无法解析时原样返回。
 */
object FeedTimeFormat {
    fun format(rawTime: String, now: Instant = Instant.now()): String = runCatching {
        val publishedAt = Instant.parse(rawTime)
        val seconds = Duration.between(publishedAt, now).seconds.coerceAtLeast(0)
        when {
            seconds < 60 -> "刚刚"
            seconds < 3_600 -> "${seconds / 60}分钟前"
            seconds < 86_400 -> "${seconds / 3_600}小时前"
            else -> DateTimeFormatter.ofPattern("MM-dd HH:mm")
                .withZone(ZoneId.systemDefault())
                .format(publishedAt)
        }
    }.getOrElse { rawTime }
}
