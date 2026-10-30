package com.kuralpay.app.voice.queue

import com.kuralpay.app.data.repository.AnnouncementRepository
import com.kuralpay.app.data.repository.SettingsRepository
import com.kuralpay.app.domain.model.VoiceProfile
import com.kuralpay.app.voice.tts.PaymentTtsEngine
import com.kuralpay.app.voice.tts.VoiceProfileMapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnnouncementQueueManager @Inject constructor(
    private val announcementRepository: AnnouncementRepository,
    private val settingsRepository: SettingsRepository,
    private val ttsEngine: PaymentTtsEngine
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private data class Pending(val text: String, val profile: VoiceProfile, val onSpoken: () -> Unit)

    private val pendingTextQueue = mutableListOf<Pending>()

    /** [onSpoken] runs only when the announcement finished playing, never on a speech error. */
    fun enqueue(text: String, profile: VoiceProfile, onSpoken: () -> Unit = {}) {
        pendingTextQueue.add(Pending(text, profile, onSpoken))
        processNext()
    }

    private fun processNext() {
        if (pendingTextQueue.isEmpty()) return
        val next = pendingTextQueue.removeFirst()
        ttsEngine.speak(next.text, next.profile) {
            next.onSpoken()
            processNext()
        }
    }

    fun replayLast() {
        scope.launch {
            val last = announcementRepository.getLastAnnouncement().first() ?: return@launch
            val profile = settingsRepository.voiceProfile.first()
            ttsEngine.speak(
                VoiceProfileMapper.getAnnouncementText(profile, last.amount, last.sender, last.sourceApp.displayName),
                profile
            )
        }
    }

    fun clear() {
        pendingTextQueue.clear()
        ttsEngine.stop()
    }
}
