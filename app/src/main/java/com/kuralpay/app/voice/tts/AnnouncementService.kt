package com.kuralpay.app.voice.tts

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.kuralpay.app.NotificationHelper
import com.kuralpay.app.data.repository.AnnouncementRepository
import com.kuralpay.app.data.repository.SettingsRepository
import com.kuralpay.app.domain.model.ParsedPayment
import com.kuralpay.app.domain.model.PaymentSource
import com.kuralpay.app.forwarding.ForwardingRepository
import com.kuralpay.app.paymentdetection.PaymentEventBus
import com.kuralpay.app.voice.queue.AnnouncementQueueManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AnnouncementService : Service() {

    @Inject lateinit var ttsEngine: PaymentTtsEngine
    @Inject lateinit var announcementRepository: AnnouncementRepository
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var queueManager: AnnouncementQueueManager
    @Inject lateinit var forwardingRepository: ForwardingRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        ttsEngine.init {
            scope.launch { observePayments() }
        }
        scope.launch { observeForwarded() }
    }

    /** Payments other KuralPay users forward to this phone join the same announcement path as local ones. */
    private suspend fun observeForwarded() {
        forwardingRepository.incomingPayments.collect { fwd ->
            if (!settingsRepository.acceptForwarded.first()) return@collect
            PaymentEventBus.emit(
                ParsedPayment(
                    amount = fwd.amount,
                    sender = fwd.payer,
                    source = PaymentSource.UNKNOWN,
                    rawText = "",
                    forwardedFrom = fwd.fromName.ifBlank { "KuralPay user" }
                )
            )
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(
            2,
            NotificationHelper.buildForegroundNotification(
                this,
                NotificationHelper.ANNOUNCEMENT_CHANNEL_ID,
                "KuralPay Voice",
                "Payment announcements are active"
            )
        )
        return START_STICKY
    }

    private suspend fun observePayments() {
        PaymentEventBus.events.collect { payment ->
            if (settingsRepository.voiceEnabled.first()) {
                val profile = settingsRepository.voiceProfile.first()
                val base = VoiceProfileMapper.getAnnouncementText(
                    profile, payment.amount, payment.sender, payment.source.displayName
                )
                val text = payment.forwardedFrom?.let { "$it. $base" } ?: base
                queueManager.enqueue(text, profile) {
                    if (payment.announcementId != 0L) {
                        scope.launch { announcementRepository.markAsReplayed(payment.announcementId) }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        ttsEngine.shutdown()
        super.onDestroy()
    }
}
