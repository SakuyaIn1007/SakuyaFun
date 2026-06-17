package com.sakuya.reader.data

import java.io.File

open class TxtLoader {
    open fun load(file: File): List<String> {
        return file.readLines()
            .chunked(20)
            .map { it.joinToString("\n")}
    }
}