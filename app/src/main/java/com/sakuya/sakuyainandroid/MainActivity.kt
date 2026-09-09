package com.sakuya.sakuyainandroid

import android.os.Bundle
import android.content.Intent
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.WindowInsetsCompat
import com.sakuya.sakuyainandroid.util.isSystemInDarkTheme
import com.sakuya.ui.theme.SakuyaInAndroidTheme
import com.sakuya.data.notification.SakuyaFirebaseMessagingService
import com.sakuya.data.notification.NotificationEntryPoint
import com.sakuya.model.notification.NotificationType
import com.sakuya.navigation.*
import com.sakuya.notification.NotificationTargetRouteMapper
import dagger.hilt.android.AndroidEntryPoint
import android.graphics.Color as AndroidColor
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val pendingNotificationRoute = mutableStateOf<String?>(null)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingNotificationRoute.value = notificationRoute(intent)
        markNotificationRead(intent)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val darkTheme by isSystemInDarkTheme()
                .collectAsState(initial = resources.configuration.isSystemInDarkTheme)

            SakuyaInAndroidTheme(darkTheme = darkTheme) {
                SideEffect {
                    configureSystemBars(darkTheme = darkTheme)
                }

                AppContent(pendingNotificationRoute.value) { pendingNotificationRoute.value = null }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingNotificationRoute.value = notificationRoute(intent)
        markNotificationRead(intent)
    }

    private fun markNotificationRead(intent: Intent?) {
        intent?.getStringExtra(SakuyaFirebaseMessagingService.EXTRA_ID)?.takeIf { it.isNotBlank() }?.let { id ->
            val entryPoint = dagger.hilt.android.EntryPointAccessors.fromApplication(applicationContext, NotificationEntryPoint::class.java)
            lifecycleScope.launch { entryPoint.notificationRepository().markRead(id) }
        }
    }

    /** 只解析服务端约定枚举并生成已有路由，未知或缺失目标统一回到通知中心。 */
    private fun notificationRoute(intent: Intent?): String? {
        val type = intent?.getStringExtra(SakuyaFirebaseMessagingService.EXTRA_TYPE)?.let { raw -> NotificationType.entries.firstOrNull { it.name == raw } } ?: return null
        val targetId = intent.getStringExtra(SakuyaFirebaseMessagingService.EXTRA_TARGET_ID).orEmpty()
        val targetUserId = intent.getStringExtra(SakuyaFirebaseMessagingService.EXTRA_TARGET_USER_ID).orEmpty()
        return NotificationTargetRouteMapper.route(type, targetId, targetUserId)
    }

    @Suppress("DEPRECATION")
    private fun configureSystemBars(darkTheme: Boolean) {
        val transparent = AndroidColor.TRANSPARENT

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                lightScrim = transparent,
                darkScrim = transparent,
                detectDarkMode = { darkTheme }
            ),
            navigationBarStyle = SystemBarStyle.auto(
                lightScrim = transparent,
                darkScrim = transparent,
                detectDarkMode = { darkTheme }
            )
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }
        WindowCompat.getInsetsController(window, window.decorView).apply {
            show(WindowInsetsCompat.Type.systemBars())
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
        }
    }
}

@Composable
private fun AppContent(pendingNotificationRoute: String?, onNotificationRouteConsumed: () -> Unit) {
    val viewModel: MainActivityViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()

    if (uiState is MainActivityUiState.Loading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val startDestination = (uiState as MainActivityUiState.Ready).startDestination
    key(startDestination) {
        MainScreen(startDestination = startDestination, pendingNotificationRoute = pendingNotificationRoute, onNotificationRouteConsumed = onNotificationRouteConsumed)
    }
}
