package com.sakuya.data.offline

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.sakuya.data.network.NetworkMonitor
import com.sakuya.data.notification.NotificationChannels
import dagger.hilt.android.qualifiers.ApplicationContext
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
 * 因容量超限被移除的离线书目。
 * 容量治理必须让用户知情：静默删除用户主动下载的内容是不可接受的。
 */
@Singleton
class OfflineEvictionEvents @Inject constructor() {
    private val mutable = MutableSharedFlow<List<String>>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val events = mutable
    fun emit(titles: List<String>) { mutable.tryEmit(titles) }
}

/**
 * OfflineDownloadCoordinator.kt
 * 职责说明：应用级消费持久化离线队列，并在启动、入队及网络恢复时自动续传。
 * 执行流程：收到信号 -> 查询可恢复任务 -> 按书串行执行；单书失败由 Repository 保留 FAILED 供手动重试。
 */
@Singleton
class OfflineDownloadCoordinator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: OfflineReadingRepository,
    private val networkMonitor: NetworkMonitor,
    private val signals: OfflineDownloadSignals,
    private val evictions: OfflineEvictionEvents,
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
        scope.launch { evictions.events.collect { titles -> notifyEvicted(titles) } }
        signals.request()
    }

    /**
     * 告知用户离线内容因容量超限被移除。
     * 阅读进度与书签不在清理范围内，文案需说明这一点，避免用户担心进度丢失。
     */
    private fun notifyEvicted(titles: List<String>) {
        if (titles.isEmpty()) return
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        NotificationChannels.create(context)
        val content = "为释放空间已移除《${titles.joinToString("》《")}》的离线内容，阅读进度与书签不受影响。"
        val notification = NotificationCompat.Builder(context, NotificationChannels.SOCIAL_CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("离线内容已自动清理")
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(context).notify(EVICTION_NOTIFICATION_ID, notification)
    }

    private companion object {
        /** 固定 ID：多次清理只保留最新一条，不堆叠通知。 */
        const val EVICTION_NOTIFICATION_ID = 0x5A4B01
    }
}
