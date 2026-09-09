package com.sakuya.data.notification

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.*
import com.sakuya.model.notification.NotificationType
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow

/** 前台收到合法 FCM Payload 时展示系统通知并通知页面刷新；未知类型不会生成可执行导航。 */
class SakuyaFirebaseMessagingService : FirebaseMessagingService() {
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
    private val entryPoint: NotificationEntryPoint by lazy { EntryPointAccessors.fromApplication(applicationContext, NotificationEntryPoint::class.java) }
    override fun onNewToken(token:String){scope.launch{entryPoint.pushRegistrationManager().registerToken(token)}}
    override fun onMessageReceived(message:RemoteMessage){
        val type=NotificationType.entries.firstOrNull{it.name==message.data[EXTRA_TYPE]}?:return
        val notificationId=message.data[EXTRA_ID].orEmpty();if(notificationId.isBlank())return
        NotificationRefreshEvents.tryEmit(Unit)
        scope.launch{entryPoint.notificationRepository().refresh()}
        showNotification(notificationId,type,message.notification?.title?:"Sakuya",message.notification?.body?:"你有一条新通知",message.data)
    }
    private fun showNotification(id:String,type:NotificationType,title:String,body:String,data:Map<String,String>){
        if(android.os.Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return
        NotificationChannels.create(this)
        val launch=packageManager.getLaunchIntentForPackage(packageName)?.apply{flags=Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP;putExtra(EXTRA_ID,id);putExtra(EXTRA_TYPE,type.name);putExtra(EXTRA_TARGET_ID,data[EXTRA_TARGET_ID]);putExtra(EXTRA_TARGET_USER_ID,data[EXTRA_TARGET_USER_ID])}?:return
        val pending=PendingIntent.getActivity(this,id.hashCode(),launch,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val value=NotificationCompat.Builder(this,NotificationChannels.SOCIAL_CHANNEL).setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle(title).setContentText(body).setStyle(NotificationCompat.BigTextStyle().bigText(body)).setAutoCancel(true).setContentIntent(pending).setPriority(NotificationCompat.PRIORITY_HIGH).build()
        NotificationManagerCompat.from(this).notify(id.hashCode(),value)
    }
    companion object { const val EXTRA_ID="notificationId";const val EXTRA_TYPE="type";const val EXTRA_TARGET_ID="targetId";const val EXTRA_TARGET_USER_ID="targetUserId" }
}

/** 前台页面订阅该事件后主动同步，避免使用全局 Android 广播暴露内部通知数据。 */
object NotificationRefreshEvents { private val events=MutableSharedFlow<Unit>(extraBufferCapacity=1);val flow=events;fun tryEmit(value:Unit)=events.tryEmit(value) }

object NotificationChannels {
    const val SOCIAL_CHANNEL="social_updates"
    fun create(context:Context){if(android.os.Build.VERSION.SDK_INT>=26){val channel=NotificationChannel(SOCIAL_CHANNEL,"动态与好友通知",NotificationManager.IMPORTANCE_HIGH).apply{description="动态互动、关注和好友关系更新"};context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)}}
}
