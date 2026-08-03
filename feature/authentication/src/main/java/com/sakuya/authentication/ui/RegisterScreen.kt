package com.sakuya.authentication.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sakuya.authentication.viewmodel.AuthUiState
import com.sakuya.ui.component.AppSecondaryTopBar
import com.sakuya.ui.theme.SakuyaInAndroidTheme

@Composable
fun RegisterScreen(
    state: AuthUiState,
    onAccountChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onNicknameChanged: (String) -> Unit,
    onRegister: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    RegisterContent(
        state = state,
        onAccountChanged = onAccountChanged,
        onPasswordChanged = onPasswordChanged,
        onNicknameChanged = onNicknameChanged,
        onRegister = onRegister,
        onBackClick = onBackClick,
        modifier = modifier
    )
}

@Composable
fun RegisterContent(
    state: AuthUiState,
    onAccountChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onNicknameChanged: (String) -> Unit,
    onRegister: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        topBar = {
            AppSecondaryTopBar(
                title = "注册账号",
                onBack = onBackClick
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 32.dp)
        ) {
            Text(
                text = "创建你的 Sakuya 账号",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(28.dp))
            OutlinedTextField(
                value = state.nickname,
                onValueChange = onNicknameChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("昵称") },
                singleLine = true,
                enabled = !state.isLoading
            )
            Spacer(modifier = Modifier.height(14.dp))
            OutlinedTextField(
                value = state.account,
                onValueChange = onAccountChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("账号") },
                singleLine = true,
                enabled = !state.isLoading
            )
            Spacer(modifier = Modifier.height(14.dp))
            OutlinedTextField(
                value = state.password,
                onValueChange = onPasswordChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("密码") },
                singleLine = true,
                enabled = !state.isLoading,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
            AuthErrorText(state.errorMessage)
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onRegister,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = !state.isLoading
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("注册")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RegisterPreview() {
    SakuyaInAndroidTheme(true) {
        RegisterScreen(
            state = AuthUiState(nickname = "Sakuya"),
            onAccountChanged = {},
            onPasswordChanged = {},
            onNicknameChanged = {},
            onRegister = {},
            onBackClick = {}
        )
    }
}
