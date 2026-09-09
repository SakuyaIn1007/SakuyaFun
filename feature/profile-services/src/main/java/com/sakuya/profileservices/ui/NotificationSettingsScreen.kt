package com.sakuya.profileservices.ui

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import com.sakuya.model.notification.NotificationPreferences
import com.sakuya.ui.component.AppSecondaryTopBar

/** 系统通知总开关与业务分类偏好页面；系统权限拒绝时明确引导用户进入 Android 设置。 */
@Composable
fun NotificationSettingsScreen(preferences:NotificationPreferences,error:String?,onBack:()->Unit,onUpdate:(NotificationPreferences)->Unit){
    val context=LocalContext.current
    var permissionGranted by remember { mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled()) }
    val launcher=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){permissionGranted=it}
    Scaffold(topBar={AppSecondaryTopBar("新消息通知",onBack)}){padding->Column(Modifier.fillMaxSize().padding(padding).padding(horizontal=16.dp)){
        if(error!=null)Text(error,color=MaterialTheme.colorScheme.error,modifier=Modifier.padding(vertical=8.dp))
        NotificationSwitchRow("系统通知总开关","控制本应用发送到通知栏的动态和好友提醒",preferences.pushEnabled){enabled->onUpdate(preferences.copy(pushEnabled=enabled));if(enabled&&Build.VERSION.SDK_INT>=33&&context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)launcher.launch(Manifest.permission.POST_NOTIFICATIONS)}
        HorizontalDivider();NotificationSwitchRow("动态互动","评论、回复、点赞与收藏",preferences.feedEnabled,preferences.pushEnabled){onUpdate(preferences.copy(feedEnabled=it))}
        HorizontalDivider();NotificationSwitchRow("好友关系","新增关注、好友申请与申请通过",preferences.friendEnabled,preferences.pushEnabled){onUpdate(preferences.copy(friendEnabled=it))}
        if(!permissionGranted){Text("Android 系统通知权限当前已关闭，应用内开关不会绕过系统设置。",color=MaterialTheme.colorScheme.error,modifier=Modifier.padding(top=20.dp));TextButton(onClick={context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,context.packageName).setData(Uri.parse("package:${context.packageName}")))}){Text("打开系统通知设置")}}
    }}
}
@Composable private fun NotificationSwitchRow(title:String,subtitle:String,checked:Boolean,enabled:Boolean=true,onChecked:(Boolean)->Unit){Row(Modifier.fillMaxWidth().padding(vertical=14.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(title,style=MaterialTheme.typography.titleSmall);Text(subtitle,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.outline)};Switch(checked,onChecked,enabled=enabled)}}
