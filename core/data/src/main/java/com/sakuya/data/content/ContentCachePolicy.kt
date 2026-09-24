package com.sakuya.data.content

/**
 * ContentCachePolicy.kt
 * 职责说明：集中定义内容接口的客户端缓存时长，供网络拦截器合成 Cache-Control 响应头。
 * 执行流程：按请求路径匹配策略 -> 返回 max-age；无法识别的路径与所有非 2xx 响应一律 no-store。
 *
 * 后端不发 Cache-Control/ETag，OkHttp 无法自行算出新鲜度，因此缓存指令必须由客户端补齐，
 * 否则响应虽被写入磁盘也永不命中。
 */
object ContentCachePolicy {

    /** 无法识别或不适合缓存的响应：既不使用已有缓存，也不写入新缓存。 */
    const val NO_STORE = "no-store"

    /** 章节正文一经发布即不可变，可长期缓存。 */
    const val CHAPTER_MAX_AGE_SECONDS = 604800L

    /** 全文会随新章发布而追加，只做短期缓存。 */
    const val FULL_CONTENT_MAX_AGE_SECONDS = 3600L

    /** 目录随作品更新变化。 */
    const val CHAPTER_INDEX_MAX_AGE_SECONDS = 1800L

    /** 书籍元数据变化频率低，但发布状态可能被后台调整。 */
    const val NOVEL_MAX_AGE_SECONDS = 3600L

    /** 搜索结果集对上游更新敏感，仅做很短的平滑缓存。 */
    const val SEARCH_MAX_AGE_SECONDS = 300L

    /**
     * 非 2xx 一律 no-store：错误响应若被缓存，恢复后会持续读到失败结果。
     * 该分支单独判断而不依赖 OkHttp 的默认行为，保证策略可被单元测试覆盖。
     */
    fun cacheControlFor(path: String, httpCode: Int): String =
        if (httpCode in 200..299) cacheControlForSuccess(path) else NO_STORE

    /** 路径为相对地址的 encodedPath，例如 /content/novels/book-1/full-content。 */
    private fun cacheControlForSuccess(path: String): String {
        val segments = path.trim('/').split('/').filter(String::isNotEmpty)
        if (segments.size < 2 || segments[0] != "content") return NO_STORE

        // 章节正文与目录比书籍元数据更具体，必须先于 /content/novels/{bookId} 判断。
        if (segments[1] == "chapters") return maxAge(CHAPTER_MAX_AGE_SECONDS)

        if (segments[1] != "novels") return NO_STORE
        return when {
            segments.size == 2 -> maxAge(SEARCH_MAX_AGE_SECONDS)
            segments.size == 3 -> maxAge(NOVEL_MAX_AGE_SECONDS)
            segments.size == 4 && segments[3] == "chapters" -> maxAge(CHAPTER_INDEX_MAX_AGE_SECONDS)
            segments.size == 4 && segments[3] == "full-content" -> maxAge(FULL_CONTENT_MAX_AGE_SECONDS)
            else -> NO_STORE
        }
    }

    private fun maxAge(seconds: Long) = "max-age=$seconds"
}
