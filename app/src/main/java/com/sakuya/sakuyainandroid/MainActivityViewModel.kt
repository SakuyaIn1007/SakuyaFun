package com.sakuya.sakuyainandroid

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sakuya.data.local.TokenStorage
import com.sakuya.navigation.AUTH_LOGIN_ROUTE
import com.sakuya.navigation.PROFILE_ROUTE
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    tokenStorage: TokenStorage,
) : ViewModel() {
    val uiState: StateFlow<MainActivityUiState> =
        if (BuildConfig.DEV_SKIP_AUTH) {
            flowOf(MainActivityUiState.Ready(PROFILE_ROUTE))
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = MainActivityUiState.Loading,
                )
        } else {
            tokenStorage.getToken()
                .map { token ->
                    val startDestination = if (token.isNullOrBlank()) {
                        AUTH_LOGIN_ROUTE
                    } else {
                        PROFILE_ROUTE
                    }
                    MainActivityUiState.Ready(startDestination)
                }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = MainActivityUiState.Loading,
                )
        }
}

sealed interface MainActivityUiState {
    data object Loading : MainActivityUiState
    data class Ready(val startDestination: String) : MainActivityUiState
}

//sealed interface MainActivityUiState {
//    data object Loading : MainActivityUiState
//
//    data class Success(val userData: UserData) : MainActivityUiState {
//        override val shouldDisableDynamicTheming = !userData.useDynamicColor
//
//        override val shouldUseAndroidTheme: Boolean = when (userData.themeBrand) {
//            ThemeBrand.DEFAULT -> false
//            ThemeBrand.ANDROID -> true
//        }
//
//        override fun shouldUseDarkTheme(isSystemDarkTheme: Boolean) =
//            when (userData.darkThemeConfig) {
//                DarkThemeConfig.FOLLOW_SYSTEM -> isSystemDarkTheme
//                DarkThemeConfig.LIGHT -> false
//                DarkThemeConfig.DARK -> true
//            }
//    }
//
//    /**
//     * Returns `true` if the state wasn't loaded yet and it should keep showing the splash screen.
//     */
//    fun shouldKeepSplashScreen() = this is Loading
//
//    /**
//     * Returns `true` if the dynamic color is disabled.
//     */
//    val shouldDisableDynamicTheming: Boolean get() = true
//
//    /**
//     * Returns `true` if the Android theme should be used.
//     */
//    val shouldUseAndroidTheme: Boolean get() = false
//
//    /**
//     * Returns `true` if dark theme should be used.
//     */
//    fun shouldUseDarkTheme(isSystemDarkTheme: Boolean) = isSystemDarkTheme
//}
