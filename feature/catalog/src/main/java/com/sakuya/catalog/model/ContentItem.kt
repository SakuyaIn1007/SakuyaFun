package com.sakuya.catalog.model

import com.sakuya.data.BuildConfig

data class ContentItem(
    val id: String,
    val title: String,
    val author: String,
    /** 上游 Wenku8 不提供出版社，JSON 中可能为 null，展示层必须容错。 */
    val publisher: String? = "",
    val rating: Float,
    val tags: List<String>,
    // Older catalog responses did not include this field, so it must remain nullable at the API boundary.
    val description: String? = null,
    val isCollected: Boolean = false,
    /** 数据来源用于避免把 Wenku8 错当成出版社；旧接口未返回时按本地书目处理。 */
    val source: String = "LOCAL",
    /** Wenku8 原始小说 ID；导航和封面均使用它，不能从本地缓存 ID 反向猜测。 */
    val sourceNovelId: String? = null,
    /** 后端网关封面路径，图片不保存到 Android 或 MySQL。 */
    val coverUrl: String? = null,
    val status: String = "",
    val copyrightRestricted: Boolean = false
) {
    val subtitle: String
        get() = "$author · ${publisher.orEmpty().ifBlank { if (source == "WENKU8") "Wenku8" else "未知来源" }}"
    /** 将后端返回的相对封面路径转换为可由 Coil 请求的认证网关地址。 */
    val coverRequestUrl: String?
        get() = coverUrl?.takeIf { it.isNotBlank() }?.let { path ->
            if (path.startsWith("http://") || path.startsWith("https://")) path
            else BuildConfig.API_BASE_URL.trimEnd('/') + "/" + path.trimStart('/')
        }
}

data class RankingItem(
    val rank: Int,
    val item: ContentItem
)

data class BannerData(
    val title: String,
    val subtitle: String,
    val badge: String
)
