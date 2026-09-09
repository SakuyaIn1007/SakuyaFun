package com.sakuya.reader.model

enum class ReaderType {
    TXT,
    EPUB,
    /** Wenku8 正文使用独立会话语义，便于章节锚点和远端恢复逻辑区分本地 TXT。 */
    WENKU8,
}
