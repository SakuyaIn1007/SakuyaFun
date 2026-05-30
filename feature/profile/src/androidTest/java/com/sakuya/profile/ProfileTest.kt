package com.sakuya.profile

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.testing.TestNavHostController
import com.sakuya.profile.ui.subpages.MyScreen
import junit.framework.TestCase.assertEquals
import org.junit.Rule
import org.junit.Test

class NavigationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun navigate_to_me() {

        lateinit var navController: TestNavHostController

        composeTestRule.setContent {

            val context = LocalContext.current

            navController = TestNavHostController(context).apply {
                navigatorProvider.addNavigator(ComposeNavigator())
            }

            NavHost(
                navController = navController,
                startDestination = "home"
            ) {

                composable("home") {

                    Button(
                        onClick = {
                            navController.navigate("profile_me")
                        }
                    ) {
                        Text("我的")
                    }
                }

                composable("profile_me") {
                    Text("My Screen")
                }
            }
        }

        composeTestRule
            .onNodeWithText("我的")
            .performClick()

        assertEquals(
            "profile_me",
            navController.currentDestination?.route
        )
    }
}