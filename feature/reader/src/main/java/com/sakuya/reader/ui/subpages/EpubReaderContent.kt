package com.sakuya.reader.ui.subpages

import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import com.sakuya.ui.theme.SakuyaInAndroidTheme

@Composable
fun EpubReaderContent(
    epubChapters: List<String>,
    fontSizeSp: Float,
    onProgress: (Float) -> Unit
) {
    if (epubChapters.isEmpty()) return

    val fullHtml = remember(epubChapters) {
        epubChapters.joinToString("<hr/>") { extractBody(it) }
    }

    val wrappedHtml = remember(fullHtml, fontSizeSp) {
        buildReaderHtml(fullHtml, fontSizeSp)
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                settings.builtInZoomControls = false
                settings.displayZoomControls = false
                addJavascriptInterface(ScrollBridge(onProgress), "ScrollBridge")
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL(null, wrappedHtml, "text/html", "UTF-8", null)
        }
    )
}

private fun extractBody(raw: String): String {
    var content = raw
        .replace(Regex("<\\?xml[^>]*\\?>", RegexOption.IGNORE_CASE), "")
        .replace(Regex("<!DOCTYPE[^>]*>", RegexOption.IGNORE_CASE), "")
        .replace(Regex("<html[^>]*>", RegexOption.IGNORE_CASE), "")
        .replace(Regex("</html>", RegexOption.IGNORE_CASE), "")
        .replace(
            Regex("<head[^>]*>.*?</head>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)),
            ""
        )
        .replace(Regex("<body[^>]*>", RegexOption.IGNORE_CASE), "")
        .replace(Regex("</body>", RegexOption.IGNORE_CASE), "")
        .trim()
    if (content.isEmpty()) {
        content = raw
    }
    return content
}

private fun buildReaderHtml(bodyHtml: String, fontSizeSp: Float): String {
    return """
<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<style>
  body {
    margin: 0;
    padding: 16px;
    font-size: ${fontSizeSp}px;
    line-height: ${fontSizeSp * 1.6f}px;
    color: #1a1a1a;
    background: #ffffff;
    word-wrap: break-word;
  }
  img { max-width: 100%; height: auto; }
</style>
</head>
<body>$bodyHtml</body>
<script>
window.addEventListener('scroll', function() {
  var max = document.documentElement.scrollHeight - window.innerHeight;
  if (max <= 0) { ScrollBridge.onProgress(0); return; }
  ScrollBridge.onProgress(window.scrollY / max);
});
</script>
</html>
""".trimIndent()
}

private class ScrollBridge(
    private val onProgress: (Float) -> Unit
) {
    @JavascriptInterface
    fun onProgress(progress: Double) {
        onProgress(progress.toFloat().coerceIn(0f, 1f))
    }
}

@Preview(showBackground = true)
@Composable
fun EpubReaderPreview() {
    SakuyaInAndroidTheme(true) {
        EpubReaderContent(
            epubChapters = listOf(
                "<h1>Chapter 1</h1><p>Enjoy reading this book with lots of content.</p>",
                "<h1>Chapter 2</h1><p>The story continues here.</p>"
            ),
            fontSizeSp = 18f,
            onProgress = {}
        )
    }
}
