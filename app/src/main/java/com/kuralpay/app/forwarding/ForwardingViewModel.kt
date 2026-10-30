package com.kuralpay.app.forwarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuralpay.app.account.AuthException
import com.kuralpay.app.account.AuthRepository
import com.kuralpay.app.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ForwardingState(
    val configured: Boolean = true,
    val signedIn: Boolean = false,
    val email: String = "",
    val myPhone: String? = null,
    val myName: String? = null,
    val requestsToMe: List<Link> = emptyList(),
    val receivingFrom: List<Link> = emptyList(),
    val sendingTo: List<Link> = emptyList(),
    val forwardingEnabled: Boolean = true,
    val acceptForwarded: Boolean = true,
    val busy: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

@HiltViewModel
class ForwardingViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val repository: ForwardingRepository,
    private val settings: SettingsRepository
) : ViewModel() {

    private val feedback = MutableStateFlow(Triple(false, null as String?, null as String?))

    val state: StateFlow<ForwardingState> = combine(
        auth.user,
        repository.myProfile,
        repository.incoming,
        repository.outgoing,
        combine(settings.forwardingEnabled, settings.acceptForwarded, feedback) { f, a, fb -> Triple(f, a, fb) }
    ) { user, profile, incoming, outgoing, (forwarding, accept, fb) ->
        ForwardingState(
            configured = auth.isConfigured,
            signedIn = user != null,
            email = user?.email.orEmpty(),
            myPhone = profile.phone,
            myName = profile.name,
            requestsToMe = incoming.filter { it.status == LinkStatus.PENDING },
            receivingFrom = incoming.filter { it.status == LinkStatus.ACCEPTED },
            sendingTo = outgoing.filter { it.status != LinkStatus.DECLINED },
            forwardingEnabled = forwarding,
            acceptForwarded = accept,
            busy = fb.first,
            message = fb.second,
            error = fb.third
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ForwardingState(configured = auth.isConfigured, signedIn = auth.currentUser != null))

    fun saveMyPhone(raw: String, name: String) = act("Saved.") { repository.setMyPhone(raw, name) }
    fun requestLink(raw: String, name: String) = act("Request sent. They'll see it under Requests.") { repository.requestLink(raw, name) }
    fun respond(link: Link, accept: Boolean) = act(null) { repository.respond(link, accept) }
    fun setMuted(link: Link, muted: Boolean) = act(null) { repository.setMuted(link, muted) }
    fun unlink(link: Link) = act(null) { repository.unlink(link) }
    fun signOut() = auth.signOut()

    fun setForwardingEnabled(on: Boolean) = viewModelScope.launch { settings.setForwardingEnabled(on) }
    fun setAcceptForwarded(on: Boolean) = viewModelScope.launch { settings.setAcceptForwarded(on) }

    fun dismissMessage() = feedback.update { Triple(it.first, null, null) }

    private fun act(success: String?, block: suspend () -> Unit) {
        viewModelScope.launch {
            feedback.value = Triple(true, null, null)
            feedback.value = try {
                block()
                Triple(false, success, null)
            } catch (e: AuthException) {
                Triple(false, null, e.message)
            } catch (e: Exception) {
                Triple(false, null, "Something went wrong. Please try again.")
            }
        }
    }
}
