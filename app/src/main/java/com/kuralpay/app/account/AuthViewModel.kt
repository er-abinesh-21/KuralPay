package com.kuralpay.app.account

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val busy: Boolean = false,
    val error: String? = null,
    val notice: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    val configured: Boolean get() = repository.isConfigured

    val signedIn: StateFlow<Boolean> = repository.user
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, repository.currentUser != null)

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun clearMessages() = _state.update { it.copy(error = null, notice = null) }

    fun signIn(email: String, password: String) {
        if (email.isBlank() || password.isEmpty()) return fail("Enter your email and password.")
        run { repository.signIn(email, password) }
    }

    fun signUp(name: String, email: String, password: String) {
        when {
            name.isBlank() -> fail("Enter your name.")
            email.isBlank() -> fail("Enter your email.")
            password.length < 6 -> fail("Choose a password with at least 6 characters.")
            else -> run { repository.signUp(name, email, password) }
        }
    }

    fun google(activityContext: Context) = run { repository.signInWithGoogle(activityContext) }

    fun resetPassword(email: String) {
        if (email.isBlank()) return fail("Enter your email above first.")
        run(success = "Reset link sent. Check your inbox.") { repository.resetPassword(email) }
    }

    private fun fail(message: String) = _state.update { it.copy(error = message, notice = null) }

    private fun run(success: String? = null, block: suspend () -> Unit) {
        viewModelScope.launch {
            _state.value = AuthUiState(busy = true)
            _state.value = try {
                block()
                AuthUiState(notice = success)
            } catch (e: AuthException) {
                AuthUiState(error = e.message)
            }
        }
    }
}
