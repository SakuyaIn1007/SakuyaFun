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

/**
 * 整本阅读：无目标章节，进入后从头连续阅读。
 * 与 WENKU8_READER_ROUTE 分开建路由，因为该路由的 chapterId/title 是必填参数，
 * 传空串会与「章节 ID 为空」这一非法输入混淆。
 */
const val WENKU8_FULL_READER_BASE_ROUTE = "reader/wenku8-full"
const val WENKU8_FULL_READER_ROUTE =
    "$WENKU8_FULL_READER_BASE_ROUTE?$READER_ARG_BOOK_ID={$READER_ARG_BOOK_ID}"
