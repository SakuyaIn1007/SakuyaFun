package com.sakuya.authentication.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sakuya.authentication.viewmodel.AuthUiState
import com.sakuya.ui.motion.MotionContent
import com.sakuya.ui.motion.MotionVisibility
import com.sakuya.ui.theme.SakuyaInAndroidTheme

@Composable
fun LoginScreen(
    state: AuthUiState,
    onAccountChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onLogin: () -> Unit,
    onRegisterClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LoginContent(
        state = state,
        onAccountChanged = onAccountChanged,
        onPasswordChanged = onPasswordChanged,
        onLogin = onLogin,
        onRegisterClick = onRegisterClick,
        modifier = modifier
    )
}

@Composable
fun LoginContent(
    state: AuthUiState,
    onAccountChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onLogin: () -> Unit,
    onRegisterClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .imePadding()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "欢迎回来",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "登录 Sakuya 继续使用",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(36.dp))
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
            onClick = onLogin,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            enabled = !state.isLoading
        ) {
            MotionContent(targetState = state.isLoading) { isLoading ->
                if (isLoading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else Text("登录")
            }
        }
        TextButton(
            onClick = onRegisterClick,
            enabled = !state.isLoading
        ) {
            Text("还没有账号？去注册")
        }
    }
}

@Composable
internal fun AuthErrorText(message: String?) {
    MotionVisibility(visible = !message.isNullOrBlank()) {
        Column {
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = message.orEmpty(),
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginPreview() {
    SakuyaInAndroidTheme(true){
        LoginScreen(
            state = AuthUiState(account = "sakuya"),
            onAccountChanged = {},
            onPasswordChanged = {},
            onLogin = {},
            onRegisterClick = {}
        )
    }
}
