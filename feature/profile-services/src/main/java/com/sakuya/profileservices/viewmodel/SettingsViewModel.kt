package com.sakuya.profileservices.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sakuya.data.local.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val sessionManager: SessionManager,
) : ViewModel() {
    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            sessionManager.logout()
            onComplete()
        }
    }
}
