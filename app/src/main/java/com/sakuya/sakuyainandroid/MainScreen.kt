package com.sakuya.sakuyainandroid

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.Icon
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import com.sakuya.navigation.FEED_COMPOSE_ROUTE
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sakuya.sakuyainandroid.navigation.AppNavHost
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
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onCreateFeed = { navController.navigate(FEED_COMPOSE_ROUTE) }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0.dp)
    ) { innerPadding ->
        AppNavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
private fun MainBottomBar(
    currentDestination: NavDestination?,
    onNavigate: (String) -> Unit,
    onCreateFeed: () -> Unit
) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.inverseOnSurface) {
        topLevelNavItems.forEachIndexed { index, item ->
            if (index == 2) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .width(58.dp)
                        .size(height = 34.dp, width = 58.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                        .clickable(onClick = onCreateFeed),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Icon(Icons.Default.Add, contentDescription = "发表动态", tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
            val selected = currentDestination.isTopLevelDestination(item.route)
            NavigationBarItem(
                selected = selected,
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = androidx.compose.ui.graphics.Color.Transparent
                ),
                onClick = { onNavigate(item.route) },
                icon = {
                    Icon(
                        modifier = Modifier
                            .size(24.dp),
                        painter = painterResource(item.iconRes),
                        contentDescription = item.label,
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        fontSize = MaterialTheme.typography.labelSmall.fontSize,
                        color = if(selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                    )
                }
            )
        }
    }
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
            onNavigate = { /* Preview 里面空实现就行喵 */ },
            onCreateFeed = {}
        )
    }
}
