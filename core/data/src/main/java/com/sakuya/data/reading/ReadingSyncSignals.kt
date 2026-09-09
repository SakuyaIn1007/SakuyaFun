package com.sakuya.data.reading

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/** 将本地写入通知给应用级协调器；信号不携带业务数据，真实队列始终以 Room 为准。 */
@Singleton
class ReadingSyncSignals @Inject constructor() {
    private val _requests = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val requests = _requests.asSharedFlow()
    fun request() { _requests.tryEmit(Unit) }
}
