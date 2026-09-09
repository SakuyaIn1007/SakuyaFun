package com.sakuya.data.notification

import android.content.Context
import com.google.firebase.messaging.FirebaseMessaging
import com.sakuya.data.local.SessionManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.UUID
import kotlin.coroutines.resume
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PushRegistrationManager.kt
 * 职责说明：维护本次安装与当前登录账号的 FCM Token 绑定。
 * 执行流程：登录或 Token 刷新时注册；退出登录前注销；失败不阻断登录，后续启动会再次补偿注册。
 */
@Singleton
class PushRegistrationManager @Inject constructor(@ApplicationContext private val context:Context,private val api:NotificationApiService,private val session:SessionManager) {
    val installationId:String by lazy { val p=context.getSharedPreferences("notification_installation",Context.MODE_PRIVATE);p.getString("id",null)?:UUID.randomUUID().toString().also{p.edit().putString("id",it).apply()} }
    suspend fun registerCurrentToken():Result<Unit> = runCatching { if(session.getTokenBlocking().isNullOrBlank())return@runCatching;registerToken(currentToken()).getOrThrow() }
    suspend fun registerToken(token:String):Result<Unit> = runCatching { if(session.getTokenBlocking().isNullOrBlank())return@runCatching;val r=api.register(DeviceRequest(installationId,token));check(r.isSuccessful&&r.body()?.isSuccess()==true){r.body()?.message?:"推送设备注册失败"} }
    suspend fun unregister():Result<Unit> = runCatching { if(session.getTokenBlocking().isNullOrBlank())return@runCatching;val r=api.unregister(installationId);check(r.isSuccessful&&r.body()?.isSuccess()==true){r.body()?.message?:"推送设备注销失败"} }
    private suspend fun currentToken():String=suspendCancellableCoroutine { continuation -> FirebaseMessaging.getInstance().token.addOnSuccessListener{continuation.resume(it)}.addOnFailureListener{continuation.cancel(it)} }
}
