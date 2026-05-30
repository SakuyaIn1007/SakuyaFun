package com.sakuya.sakuyainandroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.sakuya.ui.theme.SakuyaInAndroidTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent{
            SakuyaInAndroidTheme(true) {
                val viewModel: MainActivityViewModel = hiltViewModel()
                val uiState by viewModel.uiState.collectAsState()

                if (uiState is MainActivityUiState.Loading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                    return@SakuyaInAndroidTheme
                }

                val startDestination = (uiState as MainActivityUiState.Ready).startDestination

                MainScreen(startDestination = startDestination)
            }
        }




//        var themeSettings by mutableStateOf(
//            ThemeSettings(
//                darkTheme = resources.configuration.isSystemInDarkTheme,
//                androidTheme = Loading.shouldUseAndroidTheme,
//                disableDynamicTheming = Loading.shouldDisableDynamicTheming,
//            ),
//        )
//
//        // Update the uiState
//        lifecycleScope.launch {
//            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
//                combine(
//                    isSystemInDarkTheme(),
//                    viewModel.uiState,
//                ) { systemDark, uiState ->
//                    ThemeSettings(
//                        darkTheme = uiState.shouldUseDarkTheme(systemDark),
//                        androidTheme = uiState.shouldUseAndroidTheme,
//                        disableDynamicTheming = uiState.shouldDisableDynamicTheming,
//                    )
//                }
//                    .onEach { themeSettings = it }
//                    .map { it.darkTheme }
//                    .distinctUntilChanged()
//                    .collect { darkTheme ->
//                        trace("niaEdgeToEdge") {
//                            // Turn off the decor fitting system windows, which allows us to handle insets,
//                            // including IME animations, and go edge-to-edge.
//                            // This is the same parameters as the default enableEdgeToEdge call, but we manually
//                            // resolve whether or not to show dark theme using uiState, since it can be different
//                            // than the configuration's dark theme value based on the user preference.
//                            enableEdgeToEdge(
//                                statusBarStyle = SystemBarStyle.auto(
//                                    lightScrim = android.graphics.Color.TRANSPARENT,
//                                    darkScrim = android.graphics.Color.TRANSPARENT,
//                                ) { darkTheme },
//                                navigationBarStyle = SystemBarStyle.auto(
//                                    lightScrim = lightScrim,
//                                    darkScrim = darkScrim,
//                                ) { darkTheme },
//                            )
//                        }
//                    }
//            }
//        }
//
//        // Keep the splash screen on-screen until the UI state is loaded. This condition is
//        // evaluated each time the app needs to be redrawn so it should be fast to avoid blocking
//        // the UI.
//        splashScreen.setKeepOnScreenCondition { viewModel.uiState.value.shouldKeepSplashScreen() }


    }
}
