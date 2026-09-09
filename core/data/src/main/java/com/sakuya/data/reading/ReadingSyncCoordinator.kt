package com.sakuya.data.reading

import com.sakuya.data.local.SessionManager
import com.sakuya.data.local.TokenStorage
import com.sakuya.data.network.NetworkMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ReadingSyncCoordinator.kt
 * 职责说明：在应用进程内监听登录、网络恢复与本地待同步信号，并合并高频进度写入。
 * 执行流程：任一触发源发出事件 -> 等待 2 秒吸收连续滚动 -> 确认网络和登录 -> Repository 串行同步。
 */
@Singleton
class ReadingSyncCoordinator @Inject constructor(
    private val repository: OnlineReadingStateRepository,
    private val signals: ReadingSyncSignals,
    private val networkMonitor: NetworkMonitor,
    private val tokenStorage: TokenStorage,
    private val sessionManager: SessionManager,
) {
    private val started = AtomicBoolean(false)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @OptIn(FlowPreview::class)
    fun start() {
        if (!started.compareAndSet(false, true)) return
        scope.launch {
            // 升级前会话可能只有 JWT；启动时先恢复本地账号命名空间。
            sessionManager.currentUserId()
            merge(
                signals.requests,
                networkMonitor.isNetworkAvailable.filter { it }.map { Unit },
                tokenStorage.getToken().filterNotNull().map { Unit },
            ).debounce(2_000).collect {
                if (networkMonitor.isNetworkAvailable.value && !tokenStorage.getTokenBlocking().isNullOrBlank()) {
                    repository.syncNow()
                }
            }
        }
    }
}
