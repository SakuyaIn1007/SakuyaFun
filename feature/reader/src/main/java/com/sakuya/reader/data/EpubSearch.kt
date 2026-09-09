package com.sakuya.reader.data

import com.sakuya.reader.model.ReaderChapter

data class EpubSearchResult(val chapterIndex: Int, val chapterTitle: String, val excerpt: String, val matchOffset: Int)

/** EPUB 搜索只处理本地已解析章节；移除标签和实体后逐章匹配，结果可直接映射为章节跳转。 */
object EpubSearch {
    fun search(chapters: List<ReaderChapter>, query: String, limit: Int = 100): List<EpubSearchResult> {
        val keyword = query.trim()
        if (keyword.length < 2) return emptyList()
        return buildList {
            chapters.forEach { chapter ->
                val plain = chapter.content.toPlainText()
                var from = 0
                while (size < limit) {
                    val offset = plain.indexOf(keyword, from, ignoreCase = true)
                    if (offset < 0) break
                    val start = (offset - 28).coerceAtLeast(0)
                    val end = (offset + keyword.length + 42).coerceAtMost(plain.length)
                    add(EpubSearchResult(chapter.index, chapter.title, plain.substring(start, end).replace(Regex("\\s+"), " "), offset))
                    from = offset + keyword.length
                }
            }
        }
    }

    private fun String.toPlainText(): String = replace(Regex("<script[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), " ")
        .replace(Regex("<style[\\s\\S]*?</style>", RegexOption.IGNORE_CASE), " ")
        .replace(Regex("<[^>]+>"), " ")
        .replace("&nbsp;", " ").replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&")
}
