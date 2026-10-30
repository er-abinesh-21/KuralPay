package com.kuralpay.app.settings.ui

import android.content.Intent
import androidx.lifecycle.ViewModel
import com.kuralpay.app.voice.tts.PaymentTtsEngine
import com.kuralpay.app.voice.tts.VoiceProfileMapper
import androidx.lifecycle.viewModelScope
import com.kuralpay.app.data.repository.SettingsRepository
import com.kuralpay.app.domain.model.AudioRoute
import com.kuralpay.app.domain.model.VoiceProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsState(
    val overlayEnabled: Boolean = true,
    val voiceEnabled: Boolean = true,
    val voiceProfile: VoiceProfile = VoiceProfile.SHOP,
    val audioRoute: AudioRoute = AudioRoute.AUTO_DETECT,
    val overlayDuration: Int = 4000,
    val groqApiKey: String = "",
    val maxVolume: Boolean = false,
    val replayCount: Int = 1
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val ttsEngine: PaymentTtsEngine
) : ViewModel() {

    fun isVoiceReady(profile: VoiceProfile): Boolean = ttsEngine.isVoiceReady(profile)

    fun installVoiceIntent(): Intent = ttsEngine.installVoiceIntent()

    fun playSample(profile: VoiceProfile) {
        val text = VoiceProfileMapper.getAnnouncementText(profile, 500.0, "Arun", "Google Pay")
        ttsEngine.init { ttsEngine.speak(text, profile) }
    }

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    val voiceProfiles = VoiceProfile.entries.toList()
    val audioRoutes = AudioRoute.entries.toList()

    init {
        viewModelScope.launch {
            combine(
                settingsRepository.overlayEnabled,
                settingsRepository.voiceEnabled,
                settingsRepository.voiceProfile,
                settingsRepository.audioRoute,
                settingsRepository.overlayDuration
            ) { overlay, voice, profile, route, duration ->
                SettingsState(
                    overlayEnabled = overlay,
                    voiceEnabled = voice,
                    voiceProfile = profile,
                    audioRoute = route,
                    overlayDuration = duration
                )
            }.collect { _state.value = it }
        }
        viewModelScope.launch {
            settingsRepository.maxVolume.collect { on -> _state.update { it.copy(maxVolume = on) } }
        }
        viewModelScope.launch {
            settingsRepository.replayCount.collect { n -> _state.update { it.copy(replayCount = n) } }
        }
        viewModelScope.launch {
            settingsRepository.groqApiKey.collect { key ->
                _state.update { it.copy(groqApiKey = key) }
            }
        }
    }

    fun toggleOverlay() = viewModelScope.launch { settingsRepository.setOverlayEnabled(!_state.value.overlayEnabled) }
    fun setReplayCount(count: Int) = viewModelScope.launch { settingsRepository.setReplayCount(count) }
    fun toggleMaxVolume() = viewModelScope.launch { settingsRepository.setMaxVolume(!_state.value.maxVolume) }
    fun toggleVoice() = viewModelScope.launch { settingsRepository.setVoiceEnabled(!_state.value.voiceEnabled) }
    fun setVoiceProfile(profile: VoiceProfile) = viewModelScope.launch { settingsRepository.setVoiceProfile(profile) }
    fun setAudioRoute(route: AudioRoute) = viewModelScope.launch { settingsRepository.setAudioRoute(route) }
    fun setOverlayDuration(duration: Int) = viewModelScope.launch { settingsRepository.setOverlayDuration(duration) }
    fun setGroqApiKey(key: String) = viewModelScope.launch { settingsRepository.setGroqApiKey(key) }
}
