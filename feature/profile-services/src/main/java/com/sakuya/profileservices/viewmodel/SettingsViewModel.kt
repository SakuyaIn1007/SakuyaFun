package com.sakuya.profileservices.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.imageLoader
import com.sakuya.data.local.SessionManager
import com.sakuya.data.notification.PushRegistrationManager
import com.sakuya.data.notification.NotificationRepository
import com.sakuya.data.offline.OfflineReadingRepository
import com.sakuya.data.storage.AppStorageManager
import com.sakuya.model.notification.NotificationPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** 各缓存分区占用与清理状态。 */
data class StorageUsage(
    val offlineBytes: Long = 0L,
    val contentCacheBytes: Long = 0L,
    val imageCacheBytes: Long = 0L,
    val temporaryBytes: Long = 0L,
    val loading: Boolean = true,
    val clearing: Boolean = false,
    val message: String? = null,
) {
    /** 可安全一键清理的部分，不含用户主动下载的离线内容。 */
    val reclaimableBytes: Long get() = contentCacheBytes + imageCacheBytes + temporaryBytes
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionManager: SessionManager,
    private val pushRegistrationManager: PushRegistrationManager,
    private val notificationRepository: NotificationRepository,
    private val storageManager: AppStorageManager,
    private val offlineReading: OfflineReadingRepository,
) : ViewModel() {
    private val _notificationPreferences = MutableStateFlow(NotificationPreferences())
    val notificationPreferences = _notificationPreferences.asStateFlow()
    private val _notificationError = MutableStateFlow<String?>(null)
    val notificationError = _notificationError.asStateFlow()

    private val _storageUsage = MutableStateFlow(StorageUsage())
    val storageUsage = _storageUsage.asStateFlow()

    fun loadNotificationPreferences() { viewModelScope.launch { notificationRepository.getPreferences().onSuccess { _notificationPreferences.value = it }.onFailure { _notificationError.value = it.message ?: "通知设置加载失败" } } }
    fun updateNotificationPreferences(value: NotificationPreferences) { viewModelScope.launch { notificationRepository.updatePreferences(value).onSuccess { _notificationPreferences.value = it }.onFailure { _notificationError.value = it.message ?: "通知设置保存失败" } } }
    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            pushRegistrationManager.unregister()
            sessionManager.logout()
            onComplete()
        }
    }

    fun loadStorageUsage() {
        viewModelScope.launch {
            _storageUsage.value = measureUsage(loading = true)
        }
    }

    /**
     * 一键清理只处理可再生的缓存，绝不触碰已下载的离线内容。
     * 离线内容是用户主动下载的资产，必须走 [clearOfflineContent] 单独确认。
     */
    fun clearReclaimableCaches() {
        viewModelScope.launch {
            _storageUsage.value = _storageUsage.value.copy(clearing = true)
            storageManager.clearContentHttpCache()
            storageManager.clearTemporaryFiles()
            withContext(Dispatchers.IO) { runCatching { context.imageLoader.diskCache?.clear() } }
            _storageUsage.value = measureUsage().copy(message = "缓存已清理")
        }
    }

    fun clearOfflineContent() {
        viewModelScope.launch {
            _storageUsage.value = _storageUsage.value.copy(clearing = true)
            val result = runCatching { offlineReading.clearAllOfflineContent() }
            _storageUsage.value = measureUsage().copy(
                message = result.fold(
                    onSuccess = { count -> if (count > 0) "已移除 $count 本书的离线内容" else "没有可移除的离线内容" },
                    onFailure = { it.message ?: "离线内容清理失败" },
                )
            )
        }
    }

    /** 未登录时离线用量不可读，按 0 展示而不是让整个页面失败。 */
    private suspend fun measureUsage(loading: Boolean = false): StorageUsage = StorageUsage(
        offlineBytes = runCatching { offlineReading.totalBytes() }.getOrDefault(0L),
        contentCacheBytes = storageManager.contentHttpCacheBytes(),
        imageCacheBytes = withContext(Dispatchers.IO) { runCatching { context.imageLoader.diskCache?.size ?: 0L }.getOrDefault(0L) },
        temporaryBytes = storageManager.temporaryFilesBytes(),
        loading = loading,
    )
}
