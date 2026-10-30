package com.kuralpay.app.emergency

import com.kuralpay.app.data.repository.AnnouncementRepository
import com.kuralpay.app.data.repository.SettingsRepository
import com.kuralpay.app.data.repository.TransactionRepository
import com.kuralpay.app.domain.model.PaymentSource
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
class ReplayManager @Inject constructor(
    private val announcementRepository: AnnouncementRepository,
    private val settingsRepository: SettingsRepository,
    private val transactionRepository: TransactionRepository,
    private val ttsEngine: PaymentTtsEngine
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Speaks the most recent payments, newest first; how many comes from Settings (1 to 10). */
    fun replayLast() {
        scope.launch {
            val count = settingsRepository.replayCount.first()
            val recent = transactionRepository.getAllTransactions().first().take(count)
            if (recent.isEmpty()) return@launch
            val profile = settingsRepository.voiceProfile.first()
            ttsEngine.init {
                recent.forEach { tx ->
                    ttsEngine.speak(
                        VoiceProfileMapper.getAnnouncementText(profile, tx.amount, tx.sender, tx.sourceApp.displayName),
                        profile
                    )
                }
            }
        }
    }

    fun replayPayment(amount: Double, sender: String, source: PaymentSource) {
        scope.launch {
            val profile = settingsRepository.voiceProfile.first()
            val text = VoiceProfileMapper.getAnnouncementText(profile, amount, sender, source.displayName)
            ttsEngine.init { ttsEngine.speak(text, profile) }
        }
    }

    fun replayPending() {
        scope.launch {
            val profile = settingsRepository.voiceProfile.first()
            val pending = announcementRepository.getPendingAnnouncements().first()
            ttsEngine.init {
                pending.forEach { announcement ->
                    val text = VoiceProfileMapper.getAnnouncementText(
                        profile, announcement.amount, announcement.sender, announcement.sourceApp.displayName
                    )
                    ttsEngine.speak(text, profile) {
                        scope.launch { announcementRepository.markAsReplayed(announcement.id) }
                    }
                }
            }
        }
    }
}
