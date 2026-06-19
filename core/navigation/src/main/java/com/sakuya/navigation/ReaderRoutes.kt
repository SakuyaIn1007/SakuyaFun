package com.sakuya.navigation

const val READER_BASE_ROUTE = "reader/path"
const val READER_ARG_PATH = "path"

// 注册给 NavGraph 的完整路由
const val READER_FULL_ROUTE = "$READER_BASE_ROUTE?$READER_ARG_PATH={$READER_ARG_PATH}"