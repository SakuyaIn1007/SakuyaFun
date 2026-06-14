package com.sakuya.friend.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.sakuya.friend.ui.FriendScreen
import com.sakuya.friend.viewmodel.FriendEffect
import com.sakuya.friend.viewmodel.FriendViewModel
import com.sakuya.navigation.FRIEND_ADD_ROUTE
import com.sakuya.navigation.FRIEND_ROUTE
import com.sakuya.navigation.FNOTICE_ROUTE
import com.sakuya.navigation.GROUP_ROUTE
import com.sakuya.navigation.chatRoute

fun NavGraphBuilder.friendNavGraph(navController: NavHostController) {
    composable(FRIEND_ROUTE) {
        val viewModel: FriendViewModel = hiltViewModel()

        LaunchedEffect(Unit) {
            viewModel.effect.collect { effect ->
                when (effect) {
                    is FriendEffect.NavigateToChat -> {
                        navController.navigate(
                            chatRoute(effect.friend.id, effect.friend.name)
                        )
                    }
                    is FriendEffect.NavigateToAddFriend -> {
                        navController.navigate(FRIEND_ADD_ROUTE)
                    }
                    is FriendEffect.ShowError -> {}
                    is FriendEffect.ShowToast -> {}
                }
            }
        }

        FriendScreen(
            viewModel = viewModel,
            onBack = { navController.popBackStack() }
        )
    }
    composable(FRIEND_ADD_ROUTE) {

    }
    composable(FNOTICE_ROUTE) {

    }
    composable(GROUP_ROUTE) {

    }
}
