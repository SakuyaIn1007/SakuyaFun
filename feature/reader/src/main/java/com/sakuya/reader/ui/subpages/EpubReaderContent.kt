package com.sakuya.reader.ui.subpages

import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun EpubReaderContent(
    pages: List<String>,
    currentPage: Int
    ) {
//    AndroidView(factory = { context ->
//        WebView(context).apply {
//            settings.javaScriptEnabled = false
//            loadDataWithBaseURL(null,html,"html/html","utf-8",null)
//        }
//    })

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = false
                settings.defaultTextEncodingName = "utf-8"
            }
        },
        update = { webView ->

            val html = pages.getOrNull(currentPage) ?: ""

            webView.loadDataWithBaseURL(
                null,
                html,
                "text/html",
                "utf-8",
                null
            )
        }
    )
}

