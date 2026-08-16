package com.sakuya.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * NetworkMonitor.kt
 * 职责说明：在应用进程内持续监听可用网络，并以只读 StateFlow 提供给功能模块。
 * 执行流程：创建时读取当前网络能力并注册回调；网络可用、能力变化或丢失时更新状态，
 * 不主动触发业务请求，因此恢复网络后由页面的刷新动作决定何时重新同步。
 */
@Singleton
class NetworkMonitor @Inject constructor(@ApplicationContext context: Context) {
    private val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
    private val _isNetworkAvailable = MutableStateFlow(currentNetworkAvailable())
    val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = update(network)
        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) = update(capabilities)
        override fun onLost(network: Network) { _isNetworkAvailable.value = currentNetworkAvailable() }
    }

    init {
        connectivityManager.registerDefaultNetworkCallback(callback)
    }

    private fun update(network: Network) {
        _isNetworkAvailable.value = connectivityManager.getNetworkCapabilities(network).isInternetCapable()
    }

    private fun update(capabilities: NetworkCapabilities) {
        _isNetworkAvailable.value = capabilities.isInternetCapable()
    }

    private fun currentNetworkAvailable(): Boolean =
        connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork).isInternetCapable()

    private fun NetworkCapabilities?.isInternetCapable(): Boolean =
        this?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
}
