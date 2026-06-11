package com.sakuya.sakuyainandroid

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sakuya.authentication.navigation.AUTH_LOGIN_ROUTE
import com.sakuya.authentication.navigation.authNavGraph
import com.sakuya.conversation.navigation.conversationNavGraph
import com.sakuya.friend.navigation.friendNavGraph
import com.sakuya.home.navigation.homeNavGraph
import com.sakuya.library.navigation.libraryNavGraph
import com.sakuya.profile.navigation.PROFILE_ROUTE
import com.sakuya.profile.navigation.profileNavGraph
import com.sakuya.sakuyainandroid.navigation.topLevelNavItems
import com.sakuya.ui.theme.SakuyaInAndroidTheme

@Composable
fun MainScreen(
    startDestination: String,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val shouldShowBottomBar = topLevelNavItems.any { item ->
        currentDestination.isTopLevelDestination(item.route)
    }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (shouldShowBottomBar) {
                MainBottomBar(
                    currentDestination = currentDestination,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(PROFILE_ROUTE) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0.dp)
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            mainNavGraph(navController = navController)
        }
    }
}

@Composable
private fun MainBottomBar(
    currentDestination: NavDestination?,
    onNavigate: (String) -> Unit
) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.inverseOnSurface) {
        topLevelNavItems.forEach { item ->
            val selected = currentDestination.isTopLevelDestination(item.route)
            NavigationBarItem(
                selected = false,
                onClick = { onNavigate(item.route) },
                icon = {
                    Icon(
                        modifier = Modifier
                            .size(32.dp),
                        painter = painterResource(item.iconRes),
                        contentDescription = item.label,
                        tint = if(selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        fontSize = MaterialTheme.typography.titleSmall.fontSize,
                        color = if(selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                    )
                }
            )
        }
    }
}

private fun NavGraphBuilder.mainNavGraph(navController: NavHostController) {
    authNavGraph(
        navController = navController,
        onAuthenticated = {
            navController.navigate(PROFILE_ROUTE) {
                popUpTo(AUTH_LOGIN_ROUTE) {
                    inclusive = true
                }
            }
        }
    )
    homeNavGraph(navController)
    conversationNavGraph(navController)
    libraryNavGraph(navController)
    profileNavGraph(navController)
    friendNavGraph(navController)
}

private fun NavDestination?.isTopLevelDestination(route: String): Boolean {
    return this?.hierarchy?.any { it.route == route } == true
}



@Preview(showBackground = true)
@Composable
fun MainBottomBarPreview() {
    SakuyaInAndroidTheme(darkTheme = true) {
        val mockDestination = NavDestination("").apply {
            route = topLevelNavItems.firstOrNull()?.route
        }

        MainBottomBar(
            currentDestination = mockDestination,
            onNavigate = { /* Preview 里面空实现就行喵 */ }
        )
    }
}
