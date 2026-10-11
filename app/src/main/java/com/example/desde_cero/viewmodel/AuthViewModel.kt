package com.example.desde_cero.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.desde_cero.model.User
import com.example.desde_cero.model.UserRepository
import com.example.desde_cero.model.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)

data class RegisterUiState(
    val fullName: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val selectedRole: UserRole = UserRole.USER,
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = UserRepository(application.applicationContext)

    private val _currentUser = MutableStateFlow<User?>(repository.getCurrentUser())
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _loginState = MutableStateFlow(LoginUiState())
    val loginState: StateFlow<LoginUiState> = _loginState.asStateFlow()

    private val _registerState = MutableStateFlow(RegisterUiState())
    val registerState: StateFlow<RegisterUiState> = _registerState.asStateFlow()

    fun onLoginEmailChange(email: String) {
        _loginState.value = _loginState.value.copy(email = email, errorMessage = null)
    }

    fun onLoginPasswordChange(password: String) {
        _loginState.value = _loginState.value.copy(password = password, errorMessage = null)
    }

    fun toggleLoginPasswordVisibility() {
        _loginState.value = _loginState.value.copy(isPasswordVisible = !_loginState.value.isPasswordVisible)
    }

    fun fillDemoUser() {
        _loginState.value = _loginState.value.copy(
            email = "usuario@divisas.com",
            password = "user",
            errorMessage = null
        )
    }

    fun fillDemoAdmin() {
        _loginState.value = _loginState.value.copy(
            email = "admin@divisas.com",
            password = "admin",
            errorMessage = null
        )
    }

    fun login(): Boolean {
        val state = _loginState.value
        if (state.email.isBlank()) {
            _loginState.value = state.copy(errorMessage = "Por favor ingresa tu correo electrónico.")
            return false
        }
        if (state.password.isBlank()) {
            _loginState.value = state.copy(errorMessage = "Por favor ingresa tu contraseña.")
            return false
        }

        val result = repository.login(state.email, state.password)
        return if (result.isSuccess) {
            val user = result.getOrNull()
            _currentUser.value = user
            _loginState.value = state.copy(isSuccess = true, errorMessage = null)
            true
        } else {
            _loginState.value = state.copy(
                errorMessage = result.exceptionOrNull()?.message ?: "Error al iniciar sesión."
            )
            false
        }
    }

    fun onRegisterFullNameChange(name: String) {
        _registerState.value = _registerState.value.copy(fullName = name, errorMessage = null)
    }

    fun onRegisterEmailChange(email: String) {
        _registerState.value = _registerState.value.copy(email = email, errorMessage = null)
    }

    fun onRegisterPasswordChange(password: String) {
        _registerState.value = _registerState.value.copy(password = password, errorMessage = null)
    }

    fun onRegisterConfirmPasswordChange(confirm: String) {
        _registerState.value = _registerState.value.copy(confirmPassword = confirm, errorMessage = null)
    }

    fun onRegisterRoleChange(role: UserRole) {
        _registerState.value = _registerState.value.copy(selectedRole = role)
    }

    fun toggleRegisterPasswordVisibility() {
        _registerState.value = _registerState.value.copy(isPasswordVisible = !_registerState.value.isPasswordVisible)
    }

    fun register(): Boolean {
        val state = _registerState.value
        if (state.fullName.isBlank()) {
            _registerState.value = state.copy(errorMessage = "Por favor ingresa tu nombre completo.")
            return false
        }
        if (state.email.isBlank() || !state.email.contains("@")) {
            _registerState.value = state.copy(errorMessage = "Por favor ingresa un correo electrónico válido.")
            return false
        }
        if (state.password.length < 3) {
            _registerState.value = state.copy(errorMessage = "La contraseña debe tener al menos 3 caracteres.")
            return false
        }
        if (state.password != state.confirmPassword) {
            _registerState.value = state.copy(errorMessage = "Las contraseñas no coinciden.")
            return false
        }

        val result = repository.register(state.fullName, state.email, state.password, state.selectedRole)
        return if (result.isSuccess) {
            val registeredUser = result.getOrNull()
            repository.login(state.email, state.password)
            _currentUser.value = registeredUser
            _registerState.value = state.copy(isSuccess = true, errorMessage = null)
            true
        } else {
            _registerState.value = state.copy(
                errorMessage = result.exceptionOrNull()?.message ?: "Error al crear la cuenta."
            )
            false
        }
    }

    fun logout() {
        repository.logout()
        _currentUser.value = null
        _loginState.value = LoginUiState()
        _registerState.value = RegisterUiState()
    }

    fun clearLoginStatus() {
        _loginState.value = _loginState.value.copy(isSuccess = false, errorMessage = null)
    }

    fun clearRegisterStatus() {
        _registerState.value = _registerState.value.copy(isSuccess = false, errorMessage = null)
    }
}
