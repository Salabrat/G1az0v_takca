package com.example.taxi_application.ui.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class AuthState {
    object Idle : AuthState()
    object Authenticated : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: Boolean get() = _isLoggedIn.value

    private val _currentPhone = MutableStateFlow<String?>(null)
    val currentPhone: String? get() = _currentPhone.value

    val currentUserId: String = "dev_user_001"

    private val DEV_PHONE = "+79997770901"

    fun login(phoneNumber: String) {
        val formattedPhone = if (phoneNumber.startsWith("+")) phoneNumber 
            else "+7${phoneNumber.filter { it.isDigit() }.takeLast(10)}"

        if (formattedPhone == DEV_PHONE) {
            _isLoggedIn.value = true
            _currentPhone.value = formattedPhone
            _authState.value = AuthState.Authenticated
        } else {
            _authState.value = AuthState.Error("Неверный номер телефона (dev: $DEV_PHONE)")
        }
    }

    fun signOut() {
        _isLoggedIn.value = false
        _currentPhone.value = null
        _authState.value = AuthState.Idle
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }
}
