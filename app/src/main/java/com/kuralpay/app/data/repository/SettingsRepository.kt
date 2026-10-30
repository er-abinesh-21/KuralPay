package com.kuralpay.app.data.repository

import com.kuralpay.app.data.local.SettingsPreferences
import com.kuralpay.app.domain.model.AudioRoute
import com.kuralpay.app.domain.model.SummarySlot
import com.kuralpay.app.domain.model.VoiceProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val preferences: SettingsPreferences
) {
    val overlayEnabled: Flow<Boolean> = preferences.overlayEnabled
    val voiceEnabled: Flow<Boolean> = preferences.voiceEnabled
    val voiceProfile: Flow<VoiceProfile> = preferences.voiceProfile.map { name ->
        try { VoiceProfile.valueOf(name) } catch (_: Exception) { VoiceProfile.SHOP }
    }
    val audioRoute: Flow<AudioRoute> = preferences.audioRoute.map { name ->
        try { AudioRoute.valueOf(name) } catch (_: Exception) { AudioRoute.AUTO_DETECT }
    }
    val summary9am: Flow<Boolean> = preferences.summary9am
    val summary1pm: Flow<Boolean> = preferences.summary1pm
    val summary6pm: Flow<Boolean> = preferences.summary6pm
    val summaryClosing: Flow<Boolean> = preferences.summaryClosing
    val summaryDetails: Flow<Boolean> = preferences.summaryDetails
    val premiumEnabled: Flow<Boolean> = preferences.premiumEnabled
    val overlayDuration: Flow<Int> = preferences.overlayDuration
    val groqApiKey: Flow<String> = preferences.groqApiKey
    val maxVolume: Flow<Boolean> = preferences.maxVolume

    suspend fun setMaxVolume(enabled: Boolean) = preferences.setMaxVolume(enabled)

    fun summaryTime(slot: SummarySlot): Flow<Int> = preferences.summaryTime(slot)
    suspend fun setSummaryTime(slot: SummarySlot, minutes: Int) = preferences.setSummaryTime(slot, minutes)

    fun summaryEnabled(slot: SummarySlot): Flow<Boolean> = when (slot) {
        SummarySlot.MORNING -> summary9am
        SummarySlot.AFTERNOON -> summary1pm
        SummarySlot.EVENING -> summary6pm
        SummarySlot.CLOSING -> summaryClosing
    }

    val forwardingEnabled: Flow<Boolean> = preferences.forwardingEnabled
    val acceptForwarded: Flow<Boolean> = preferences.acceptForwarded
    suspend fun setForwardingEnabled(enabled: Boolean) = preferences.setForwardingEnabled(enabled)
    suspend fun setAcceptForwarded(enabled: Boolean) = preferences.setAcceptForwarded(enabled)

    val replayCount: Flow<Int> = preferences.replayCount
    suspend fun setReplayCount(count: Int) = preferences.setReplayCount(count)

    suspend fun setOverlayEnabled(enabled: Boolean) = preferences.setOverlayEnabled(enabled)
    suspend fun setVoiceEnabled(enabled: Boolean) = preferences.setVoiceEnabled(enabled)
    suspend fun setVoiceProfile(profile: VoiceProfile) = preferences.setVoiceProfile(profile.name)
    suspend fun setAudioRoute(route: AudioRoute) = preferences.setAudioRoute(route.name)
    suspend fun setSummary9am(enabled: Boolean) = preferences.setSummary9am(enabled)
    suspend fun setSummary1pm(enabled: Boolean) = preferences.setSummary1pm(enabled)
    suspend fun setSummary6pm(enabled: Boolean) = preferences.setSummary6pm(enabled)
    suspend fun setSummaryClosing(enabled: Boolean) = preferences.setSummaryClosing(enabled)
    suspend fun setSummaryDetails(enabled: Boolean) = preferences.setSummaryDetails(enabled)
    suspend fun setOverlayDuration(duration: Int) = preferences.setOverlayDuration(duration)
    suspend fun setGroqApiKey(key: String) = preferences.setGroqApiKey(key)
}
