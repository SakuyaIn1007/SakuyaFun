package com.sakuya.authentication.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.sakuya.authentication.ui.LoginScreen
import com.sakuya.authentication.ui.RegisterScreen
import com.sakuya.authentication.viewmodel.AuthEffect
import com.sakuya.authentication.viewmodel.AuthViewModel
import com.sakuya.navigation.AUTH_LOGIN_ROUTE
import com.sakuya.navigation.AUTH_REGISTER_ROUTE

fun NavGraphBuilder.authNavGraph(
    navController: NavHostController,
    onAuthenticated: () -> Unit
) {
    composable(AUTH_LOGIN_ROUTE) {
        val viewModel: AuthViewModel = hiltViewModel()
        val state by viewModel.loginState.collectAsState()

        LaunchedEffect(viewModel) {
            viewModel.effect.collect { effect ->
                if (effect is AuthEffect.Authenticated) {
                    onAuthenticated()
                }
            }
        }

        LoginScreen(
            state = state,
            onAccountChanged = viewModel::onLoginAccountChanged,
            onPasswordChanged = viewModel::onLoginPasswordChanged,
            onLogin = viewModel::login,
            onRegisterClick = { navController.navigate(AUTH_REGISTER_ROUTE) }
        )
    }

    composable(AUTH_REGISTER_ROUTE) {
        val viewModel: AuthViewModel = hiltViewModel()
        val state by viewModel.registerState.collectAsState()

        LaunchedEffect(viewModel) {
            viewModel.effect.collect { effect ->
                if (effect is AuthEffect.Authenticated) {
                    onAuthenticated()
                }
            }
        }

        RegisterScreen(
            state = state,
            onAccountChanged = viewModel::onRegisterAccountChanged,
            onPasswordChanged = viewModel::onRegisterPasswordChanged,
            onNicknameChanged = viewModel::onRegisterNicknameChanged,
            onRegister = viewModel::register,
            onBackClick = { navController.popBackStack() }
        )
    }
}
