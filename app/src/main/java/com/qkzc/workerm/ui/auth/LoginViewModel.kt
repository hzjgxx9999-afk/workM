package com.qkzc.workerm.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.qkzc.workerm.data.session.AuthRepository
import com.qkzc.workerm.data.session.LoginPayload
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val loading: Boolean = false,
    val loggedIn: Boolean = false,
    val errorMessage: String? = null,
)

class LoginViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun login(mobile: String, password: String) {
        _uiState.update { it.copy(loading = true, errorMessage = null, loggedIn = false) }
        viewModelScope.launch {
            runCatching {
                authRepository.login(
                    LoginPayload(
                        mobile = mobile,
                        password = password,
                    ),
                )
            }.onSuccess {
                _uiState.update { it.copy(loading = false, loggedIn = true) }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        loading = false,
                        errorMessage = throwable.message ?: "登录失败，请稍后重试",
                    )
                }
            }
        }
    }

    fun consumeError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun consumeLoggedIn() {
        _uiState.update { it.copy(loggedIn = false) }
    }

    class Factory(
        private val authRepository: AuthRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LoginViewModel(authRepository) as T
        }
    }
}
