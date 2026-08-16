package com.sakuya.navigation

const val READER_BASE_ROUTE = "reader/path"
const val READER_ARG_PATH = "path"
const val READER_ARG_BOOK_ID = "bookId"
const val READER_ARG_CHAPTER_ID = "chapterId"
const val READER_ARG_TITLE = "title"

// 注册给 NavGraph 的完整路由
const val READER_FULL_ROUTE =
    "$READER_BASE_ROUTE?$READER_ARG_BOOK_ID={$READER_ARG_BOOK_ID}&$READER_ARG_PATH={$READER_ARG_PATH}"

/** 远端章节与本地文件分开建路由，避免将章节 ID 误当作磁盘路径。 */
const val WENKU8_READER_BASE_ROUTE = "reader/wenku8"
const val WENKU8_READER_ROUTE = "$WENKU8_READER_BASE_ROUTE?$READER_ARG_BOOK_ID={$READER_ARG_BOOK_ID}&$READER_ARG_CHAPTER_ID={$READER_ARG_CHAPTER_ID}&$READER_ARG_TITLE={$READER_ARG_TITLE}"
