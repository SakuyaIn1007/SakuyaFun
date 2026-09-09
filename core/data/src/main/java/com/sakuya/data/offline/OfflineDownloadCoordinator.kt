package com.sakuya.data.offline

import com.sakuya.data.network.NetworkMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineDownloadSignals @Inject constructor() {
    private val mutable = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val requests = mutable
    fun request() { mutable.tryEmit(Unit) }
}

/**
 * OfflineDownloadCoordinator.kt
 * 职责说明：应用级消费持久化离线队列，并在启动、入队及网络恢复时自动续传。
 * 执行流程：收到信号 -> 查询可恢复任务 -> 按书串行执行；单书失败由 Repository 保留 FAILED 供手动重试。
 */
@Singleton
class OfflineDownloadCoordinator @Inject constructor(
    private val repository: OfflineReadingRepository,
    private val networkMonitor: NetworkMonitor,
    private val signals: OfflineDownloadSignals,
) {
    private val started = AtomicBoolean(false)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    fun start() {
        if (!started.compareAndSet(false, true)) return
        scope.launch {
            merge(signals.requests, networkMonitor.isNetworkAvailable.filter { it }).collectLatest {
                if (networkMonitor.isNetworkAvailable.value) repository.resumable().forEach { repository.execute(it) }
            }
        }
        signals.request()
    }
}
