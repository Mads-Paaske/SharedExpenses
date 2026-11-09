package com.example.sharedexpenses.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sharedexpenses.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AuthError { WRONG_LOGIN, MISSING_FIELDS, EMAIL_TAKEN }

/** Log in and register (mock authentication, F01 + F04). */
class AuthViewModel(private val userRepository: UserRepository) : ViewModel() {
    private val _error = MutableStateFlow<AuthError?>(null)
    val error: StateFlow<AuthError?> = _error.asStateFlow()

    fun logIn(email: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            if (userRepository.logIn(email, password)) {
                _error.value = null
                onSuccess()
            } else {
                _error.value = AuthError.WRONG_LOGIN
            }
        }
    }

    fun register(name: String, email: String, phone: String, password: String, onSuccess: () -> Unit) {
        if (name.isBlank() || email.isBlank() || password.isBlank()) {
            _error.value = AuthError.MISSING_FIELDS
            return
        }
        viewModelScope.launch {
            if (userRepository.register(name, email, phone, password)) {
                _error.value = null
                onSuccess()
            } else {
                _error.value = AuthError.EMAIL_TAKEN
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
