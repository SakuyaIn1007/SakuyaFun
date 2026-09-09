package com.sakuya.profileservices.navigation

import androidx.navigation.NavGraphBuilder
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.sakuya.designsystem.icon.SakuyaIcons
import com.sakuya.navigation.AUTH_LOGIN_ROUTE
import com.sakuya.navigation.PROFILE_ALBUMS_ROUTE
import com.sakuya.navigation.PROFILE_CARDS_ROUTE
import com.sakuya.navigation.PROFILE_FAVOURITES_ROUTE
import com.sakuya.navigation.PROFILE_ME
import com.sakuya.navigation.PROFILE_PRIVACY_ROUTE
import com.sakuya.navigation.PROFILE_SETTINGS_ROUTE
import com.sakuya.navigation.PROFILE_NOTIFICATION_SETTINGS_ROUTE

import com.sakuya.profileservices.data.GroupedWallet
import com.sakuya.profileservices.data.WalletItem
import com.sakuya.profileservices.data.remote.FavouritesItemData
import com.sakuya.profileservices.ui.AlbumMainScreen
import com.sakuya.profileservices.ui.CardsScreen
import com.sakuya.profileservices.ui.FavouritesScreen
import com.sakuya.profileservices.ui.SettingsScreen
import com.sakuya.profileservices.ui.NotificationSettingsScreen
import androidx.compose.runtime.*
import com.sakuya.profileservices.viewmodel.SettingsViewModel
import java.util.Date

fun NavGraphBuilder.profileServicesNavGraph(navController: NavHostController) {
    composable(PROFILE_FAVOURITES_ROUTE) {
        FavouritesScreen(
            favourites = sampleFavourites(),
            onBack = { navController.popBackStack() }
        )
    }
    composable(PROFILE_ALBUMS_ROUTE) {
        AlbumMainScreen(onBack = { navController.popBackStack() })
    }
    composable(PROFILE_CARDS_ROUTE) {
        CardsScreen(onBack = { navController.popBackStack() })
    }
    composable(PROFILE_SETTINGS_ROUTE) {
        val viewModel: SettingsViewModel = hiltViewModel()
        val goLogin = {
            navController.navigate(AUTH_LOGIN_ROUTE) {
                popUpTo(navController.graph.findStartDestination().id) {
                    inclusive = true
                }
                launchSingleTop = true
            }
        }
        SettingsScreen(
            onBack = { navController.popBackStack() },
            onProfileClick = { navController.navigate(PROFILE_ME) },
            onPrivacyClick = { navController.navigate(PROFILE_PRIVACY_ROUTE) },
            onNotificationClick = { navController.navigate(PROFILE_NOTIFICATION_SETTINGS_ROUTE) },
            onSwitchAccount = { viewModel.logout(goLogin) },
            onLogout = { viewModel.logout(goLogin) },
        )
    }
    composable(PROFILE_NOTIFICATION_SETTINGS_ROUTE) {
        val viewModel: SettingsViewModel = hiltViewModel()
        val preferences by viewModel.notificationPreferences.collectAsState()
        val error by viewModel.notificationError.collectAsState()
        LaunchedEffect(Unit) { viewModel.loadNotificationPreferences() }
        NotificationSettingsScreen(preferences,error,{navController.popBackStack()},viewModel::updateNotificationPreferences)
    }
}

private fun sampleWalletGroups() = listOf(
    GroupedWallet(
        groupName = "金融理财",
        wallets = listOf(
            WalletItem(SakuyaIcons.Wallet, "银行卡"),
            WalletItem(SakuyaIcons.Cards, "卡包"),
            WalletItem(SakuyaIcons.Favorites, "票券"),
        )
    ),
    GroupedWallet(
        groupName = "生活服务",
        wallets = listOf(
            WalletItem(SakuyaIcons.Wallet, "充值"),
            WalletItem(SakuyaIcons.Cards, "账单"),
            WalletItem(SakuyaIcons.Favorites, "收藏服务"),
        )
    )
)

private fun sampleFavourites() = listOf(
    FavouritesItemData(
        title = "收藏内容",
        imageUrl = "https://picsum.photos/300?random=41",
        resources = "Sakuya",
        date = Date()
    )
)
