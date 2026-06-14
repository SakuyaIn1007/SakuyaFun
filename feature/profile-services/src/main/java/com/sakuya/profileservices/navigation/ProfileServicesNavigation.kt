package com.sakuya.profileservices.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.sakuya.designsystem.icon.SakuyaIcons
import com.sakuya.navigation.PROFILE_ALBUMS_ROUTE
import com.sakuya.navigation.PROFILE_CARDS_ROUTE
import com.sakuya.navigation.PROFILE_FAVOURITES_ROUTE
import com.sakuya.navigation.PROFILE_SETTINGS_ROUTE
import com.sakuya.navigation.PROFILE_WALLET_ROUTE
import com.sakuya.profileservices.data.GroupedWallet
import com.sakuya.profileservices.data.WalletItem
import com.sakuya.profileservices.data.remote.FavouritesItemData
import com.sakuya.profileservices.ui.AlbumMainScreen
import com.sakuya.profileservices.ui.CardsScreen
import com.sakuya.profileservices.ui.FavouritesScreen
import com.sakuya.profileservices.ui.SettingsScreen
import com.sakuya.profileservices.ui.WalletContent
import java.util.Date

fun NavGraphBuilder.profileServicesNavGraph(navController: NavHostController) {
    composable(PROFILE_WALLET_ROUTE) {
        WalletContent(
            groups = sampleWalletGroups(),
            onBack = { navController.popBackStack() }
        )
    }
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
        SettingsScreen(onBack = { navController.popBackStack() })
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
