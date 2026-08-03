package com.sakuya.sakuyainandroid

import android.os.Bundle
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.WindowInsetsCompat
import com.sakuya.sakuyainandroid.util.isSystemInDarkTheme
import com.sakuya.ui.theme.SakuyaInAndroidTheme
import dagger.hilt.android.AndroidEntryPoint
import android.graphics.Color as AndroidColor

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val darkTheme by isSystemInDarkTheme()
                .collectAsState(initial = resources.configuration.isSystemInDarkTheme)

            SakuyaInAndroidTheme(darkTheme = darkTheme) {
                SideEffect {
                    configureSystemBars(darkTheme = darkTheme)
                }

                AppContent()
            }
        }
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
private fun AppContent() {
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
        MainScreen(startDestination = startDestination)
    }
}
