package com.sakuya.sakuyainandroid

import android.app.Application
import android.content.pm.ApplicationInfo
import android.os.StrictMode
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.sakuya.data.remote.AuthenticatedImageLoaderEntryPoint
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.android.EntryPointAccessors

@HiltAndroidApp
/**
 * SiaApplication.kt
 * 职责说明：初始化应用级基础能力，并为 Coil 提供带 JWT 的封面下载客户端。
 * 执行流程：Compose 图片组件委托 Coil -> Coil 从本类取得 ImageLoader -> OkHttp 自动附加当前登录令牌。
 */
class SiaApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
//启动严苛模式
        setStrictModePolicy()
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
