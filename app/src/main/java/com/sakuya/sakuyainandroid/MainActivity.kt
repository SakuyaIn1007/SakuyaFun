package com.sakuya.sakuyainandroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.sakuya.ui.theme.SakuyaInAndroidTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            SakuyaInAndroidTheme(true) {
                val systemUiController = rememberSystemUiController()

                SideEffect {
                    systemUiController.apply {
                        setStatusBarColor(
                            color = Color.Transparent,
                            darkIcons = true
                        )
                        setNavigationBarColor(
                            color = Color.Transparent,
                            darkIcons = false,
                            navigationBarContrastEnforced = false
                        )
                        isStatusBarVisible = true
                        isNavigationBarVisible = false
                        systemBarsBehavior =
                            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    }
                }

                AppContent()
            }
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
    MainScreen(startDestination = startDestination)
}
