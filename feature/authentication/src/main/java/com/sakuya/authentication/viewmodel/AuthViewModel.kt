package com.sakuya.authentication.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sakuya.authentication.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _loginState = MutableStateFlow(AuthUiState())
    val loginState = _loginState.asStateFlow()

    private val _registerState = MutableStateFlow(AuthUiState())
    val registerState = _registerState.asStateFlow()

    private val _effect = MutableSharedFlow<AuthEffect>()
    val effect = _effect.asSharedFlow()

    fun onLoginAccountChanged(value: String) {
        _loginState.update { it.copy(account = value, errorMessage = null) }
    }

    fun onLoginPasswordChanged(value: String) {
        _loginState.update { it.copy(password = value, errorMessage = null) }
    }

    fun onRegisterAccountChanged(value: String) {
        _registerState.update { it.copy(account = value, errorMessage = null) }
    }

    fun onRegisterPasswordChanged(value: String) {
        _registerState.update { it.copy(password = value, errorMessage = null) }
    }

    fun onRegisterNicknameChanged(value: String) {
        _registerState.update { it.copy(nickname = value, errorMessage = null) }
    }

    fun login() {
        val state = _loginState.value
        val validationError = validateAccountAndPassword(state.account, state.password)
        if (validationError != null) {
            _loginState.update { it.copy(errorMessage = validationError) }
            return
        }

        viewModelScope.launch {
            _loginState.update { it.copy(isLoading = true, errorMessage = null) }
            authRepository.login(state.account.trim(), state.password)
                .onSuccess {
                    _loginState.update { it.copy(isLoading = false) }
                    _effect.emit(AuthEffect.Authenticated)
                }
                .onFailure { error ->
                    _loginState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "登录失败"
                        )
                    }
                }
        }
    }

    fun register() {
        Log.d("AuthViewModel","1")
        val state = _registerState.value
        val validationError = validateAccountAndPassword(state.account, state.password)
            ?: if (state.nickname.isBlank()) "请输入昵称" else null
        if (validationError != null) {
            _registerState.update { it.copy(errorMessage = validationError) }
            return
        }
        Log.d("AuthViewModel","2")
        viewModelScope.launch {
            _registerState.update { it.copy(isLoading = true, errorMessage = null) }
            authRepository.register(
                account = state.account.trim(),
                password = state.password,
                nickname = state.nickname.trim()
            )
                .onSuccess {
                    _registerState.update { it.copy(isLoading = false) }
                    _effect.emit(AuthEffect.Authenticated)
                }
                .onFailure { error ->
                    _registerState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "注册失败"
                        )
                    }
                }
        }
    }

    private fun validateAccountAndPassword(account: String, password: String): String? {
        return when {
            account.isBlank() -> "请输入账号"
            password.length < 6 -> "密码至少 6 位"
            else -> null
        }
    }
}

data class AuthUiState(
    val account: String = "",
    val password: String = "",
    val nickname: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

sealed interface AuthEffect {
    data object Authenticated : AuthEffect
}
