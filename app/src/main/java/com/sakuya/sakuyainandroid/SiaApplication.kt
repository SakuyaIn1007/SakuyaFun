package com.sakuya.sakuyainandroid

import android.app.Application
import android.content.pm.ApplicationInfo
import android.os.StrictMode
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SiaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
//启动严苛模式
        setStrictModePolicy()
//        Sync.initialize(context = this)
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
