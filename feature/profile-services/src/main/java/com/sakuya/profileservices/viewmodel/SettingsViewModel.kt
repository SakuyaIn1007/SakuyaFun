package com.sakuya.profileservices.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sakuya.data.local.SessionManager
import com.sakuya.data.notification.PushRegistrationManager
import com.sakuya.data.notification.NotificationRepository
import com.sakuya.model.notification.NotificationPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val pushRegistrationManager: PushRegistrationManager,
    private val notificationRepository: NotificationRepository,
) : ViewModel() {
    private val _notificationPreferences = MutableStateFlow(NotificationPreferences())
    val notificationPreferences = _notificationPreferences.asStateFlow()
    private val _notificationError = MutableStateFlow<String?>(null)
    val notificationError = _notificationError.asStateFlow()

    fun loadNotificationPreferences() { viewModelScope.launch { notificationRepository.getPreferences().onSuccess { _notificationPreferences.value = it }.onFailure { _notificationError.value = it.message ?: "通知设置加载失败" } } }
    fun updateNotificationPreferences(value: NotificationPreferences) { viewModelScope.launch { notificationRepository.updatePreferences(value).onSuccess { _notificationPreferences.value = it }.onFailure { _notificationError.value = it.message ?: "通知设置保存失败" } } }
    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            pushRegistrationManager.unregister()
            sessionManager.logout()
            onComplete()
        }
    }
}
