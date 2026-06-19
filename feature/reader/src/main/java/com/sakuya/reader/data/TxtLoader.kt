package com.sakuya.reader.data

import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import javax.inject.Inject

open class TxtLoader @Inject constructor() {
    open fun load(inputStream: InputStream): String {
        return inputStream.bufferedReader(Charsets.UTF_8).readText()
    }
}