package com.sakuya.sakuyainandroid

import android.app.Application
import android.content.pm.ApplicationInfo
import android.os.StrictMode
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.sakuya.data.remote.AuthenticatedImageLoaderEntryPoint
import com.sakuya.data.notification.NotificationChannels
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.android.EntryPointAccessors
import com.sakuya.data.notification.NotificationEntryPoint
import com.sakuya.data.reading.ReadingSyncCoordinator
import com.sakuya.data.offline.OfflineDownloadCoordinator
import kotlinx.coroutines.*
import javax.inject.Inject

@HiltAndroidApp
/**
 * SiaApplication.kt
 * 职责说明：初始化应用级基础能力，并为 Coil 提供带 JWT 的封面下载客户端。
 * 执行流程：Compose 图片组件委托 Coil -> Coil 从本类取得 ImageLoader -> OkHttp 自动附加当前登录令牌。
 */
class SiaApplication : Application(), ImageLoaderFactory {
    @Inject lateinit var readingSyncCoordinator: ReadingSyncCoordinator
    @Inject lateinit var offlineDownloadCoordinator: OfflineDownloadCoordinator
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    override fun onCreate() {
        super.onCreate()
//启动严苛模式
        setStrictModePolicy()
        NotificationChannels.create(this)
        // 升级或系统恢复后补偿设备注册；失败不影响应用启动，后续 Token 刷新仍会重试。
        val notificationEntryPoint = EntryPointAccessors.fromApplication(this, NotificationEntryPoint::class.java)
        applicationScope.launch { notificationEntryPoint.pushRegistrationManager().registerCurrentToken() }
        readingSyncCoordinator.start()
        offlineDownloadCoordinator.start()
//        Sync.initialize(context = this)
    }

    override fun newImageLoader(): ImageLoader {
        val network = EntryPointAccessors.fromApplication(this, AuthenticatedImageLoaderEntryPoint::class.java)
        return ImageLoader.Builder(this)
            .okHttpClient { network.okHttpClient() }
            .build()
    }

//    判断是不是测试版本
    private fun isDebuggable(): Boolean {
        return 0 != applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE
    }


    private fun setStrictModePolicy(){
        if(isDebuggable()){
            StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build())

        }
    }
}
