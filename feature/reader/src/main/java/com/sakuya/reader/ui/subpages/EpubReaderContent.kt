package com.sakuya.reader.ui.subpages

import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.sakuya.reader.model.ReaderReadingPosition
import com.sakuya.reader.model.ReaderTheme
import com.sakuya.reader.model.ReaderType
import com.sakuya.ui.theme.SakuyaInAndroidTheme
import kotlin.math.roundToInt

/**
 * EpubReaderContent.kt
 * 职责说明：按 EPUB 章节加载受控 HTML，并在不暴露 JavaScript 接口的前提下回传阅读位置。
 * 执行流程：ViewModel 提供已解析章节和恢复位置 -> 本组件只载入当前章的已清洗 HTML ->
 * WebView 原生滚动回调计算章节内比例 -> 上层持久化统一 ReaderReadingPosition。
 */
@Composable
fun EpubReaderContent(
    epubChapters: List<String>,
    fontSizeSp: Float,
    initialProgress: Float,
    onProgress: (Float) -> Unit,
    initialChapterIndex: Int = 0,
    initialChapterProgress: Float = initialProgress,
    readerTheme: ReaderTheme = ReaderTheme.SYSTEM,
    restoreKey: Long = 0L,
    onReadingPosition: (ReaderReadingPosition) -> Unit = {},
) {
    if (epubChapters.isEmpty()) return

    var chapterIndex by rememberSaveable(epubChapters, restoreKey) {
        mutableIntStateOf(initialChapterIndex.coerceIn(0, epubChapters.lastIndex))
    }
    val currentOnProgress = rememberUpdatedState(onProgress)
    val currentOnReadingPosition = rememberUpdatedState(onReadingPosition)
    val chapterProgress = initialChapterProgress.coerceIn(0f, 1f)
    val palette = readerContentPalette(readerTheme)

    LaunchedEffect(restoreKey, epubChapters.size) {
        // 旧数据没有章节信息时，以全书比例回退到最接近的章节。
        chapterIndex = if (initialChapterIndex in epubChapters.indices) initialChapterIndex
        else (initialProgress.coerceIn(0f, 1f) * epubChapters.lastIndex).roundToInt()
    }

    val currentHtml = remember(epubChapters, chapterIndex, fontSizeSp, readerTheme) {
        buildReaderHtml(
            bodyHtml = sanitizeEpubHtml(epubChapters[chapterIndex]),
            fontSizeSp = fontSizeSp,
            background = palette.background.toReaderCssColor(),
            content = palette.content.toReaderCssColor(),
        )
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                /** EPUB 属于不受信任导入内容：不执行脚本、不允许访问文件/网络，也不允许新窗口。 */
                settings.javaScriptEnabled = false
                settings.javaScriptCanOpenWindowsAutomatically = false
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.blockNetworkLoads = true
                settings.setSupportMultipleWindows(false)
                settings.builtInZoomControls = false
                settings.displayZoomControls = false
                isVerticalScrollBarEnabled = false
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = true

                    override fun onPageFinished(view: WebView, url: String?) {
                        view.post {
                            val range = (view.contentHeight * view.scale - view.height).toInt().coerceAtLeast(0)
                            view.scrollTo(0, (range * chapterProgress).roundToInt())
                        }
                    }
                }
                setOnScrollChangeListener { view, _, scrollY, _, _ ->
                    val webView = view as WebView
                    val range = (webView.contentHeight * webView.scale - webView.height).toInt().coerceAtLeast(0)
                    val localProgress = if (range == 0) 0f else (scrollY.toFloat() / range).coerceIn(0f, 1f)
                    val globalProgress = ((chapterIndex + localProgress) / epubChapters.size).coerceIn(0f, 1f)
                    currentOnProgress.value(globalProgress)
                    currentOnReadingPosition.value(
                        ReaderReadingPosition(
                            type = ReaderType.EPUB,
                            progress = globalProgress,
                            chapterIndex = chapterIndex,
                            chapterProgress = localProgress,
                        )
                    )
                }
            }
        },
        update = { webView ->
            if (webView.tag != currentHtml) {
                webView.tag = currentHtml
                webView.loadDataWithBaseURL(null, currentHtml, "text/html", "UTF-8", null)
            }
        },
    )
}

/**
 * 去除脚本、事件属性、表单、外链与图片资源。导入 EPUB 不可借由阅读器加载外部页面或本地文件；
 * 首期宁可不展示未验证图片，也不向 WebView 开放资源权限。
 */
internal fun sanitizeEpubHtml(raw: String): String = raw
    .replace(Regex("<\\?xml[^>]*\\?>|<!DOCTYPE[^>]*>", RegexOption.IGNORE_CASE), "")
    .replace(Regex("<(script|style|iframe|object|embed|form|link|base|img)[^>]*>.*?</\\1\\s*>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "")
    .replace(Regex("<(script|style|iframe|object|embed|form|link|base|img)[^>]*/?>", RegexOption.IGNORE_CASE), "")
    .replace(Regex("\\s+on[a-zA-Z]+\\s*=\\s*(?:\"[^\"]*\"|'[^']*'|[^\\s>]+)", RegexOption.IGNORE_CASE), "")
    .replace(Regex("\\s+(?:src|href)\\s*=\\s*(?:\"[^\"]*\"|'[^']*'|[^\\s>]+)", RegexOption.IGNORE_CASE), "")
    .replace(Regex("<head[^>]*>.*?</head>|</?(?:html|body)[^>]*>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "")
    .trim()

/** 仅注入阅读排版 CSS；不包含脚本和外部资源地址。 */
internal fun buildReaderHtml(bodyHtml: String, fontSizeSp: Float, background: String, content: String): String = """
    <!DOCTYPE html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1.0">
    <style>body{margin:0;padding:16px;font-size:${fontSizeSp}px;line-height:${fontSizeSp * 1.6f}px;color:$content;background:$background;word-wrap:break-word} img{display:none}</style>
    </head><body>$bodyHtml</body></html>
""".trimIndent()

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun EpubReaderPreview() {
    SakuyaInAndroidTheme(true) {
        EpubReaderContent(
            epubChapters = listOf("<h1>第一章</h1><p>安全的 EPUB 正文预览。</p>"),
            fontSizeSp = 18f,
            initialProgress = 0f,
            onProgress = {},
        )
    }
}
