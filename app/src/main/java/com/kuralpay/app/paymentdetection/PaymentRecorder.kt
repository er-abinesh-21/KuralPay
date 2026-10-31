package com.kuralpay.app.paymentdetection

import android.util.Log
import com.kuralpay.app.data.local.dao.TransactionDao
import com.kuralpay.app.data.repository.AnnouncementRepository
import com.kuralpay.app.data.repository.SettingsRepository
import com.kuralpay.app.data.repository.TransactionRepository
import com.kuralpay.app.data.sync.CloudSyncRepository
import com.kuralpay.app.domain.model.Announcement
import com.kuralpay.app.domain.model.ParsedPayment
import com.kuralpay.app.domain.model.Transaction
import com.kuralpay.app.forwarding.ForwardingRepository
import com.kuralpay.app.paymentdetection.parser.PaymentMessageParser
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Records a detected payment once, whichever way it was found (live notification, notification
 * shade after a restart, or the SMS inbox), then backs it up, queues the announcement and forwards it.
 */
@Singleton
class PaymentRecorder @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val transactionDao: TransactionDao,
    private val announcementRepository: AnnouncementRepository,
    private val forwardingRepository: ForwardingRepository,
    private val cloudSync: CloudSyncRepository,
    private val settingsRepository: SettingsRepository
) {
    /** Checking for a duplicate and recording must not interleave when two copies arrive together. */
    private val lock = Mutex()

    /**
     * @param receivedAt when the payment arrived (notification post time or SMS time).
     * @param speak read it out now; false leaves it under "Replay missed" (found too late to announce).
     * @return true if it was new and recorded.
     */
    suspend fun record(parsed: ParsedPayment, receivedAt: Long, speak: Boolean, origin: String): Boolean {
        val isNew = lock.withLock {
            // Survives Android stopping the app: same notification, or same bank reference sent twice.
            if (transactionDao.countSameNotification(parsed.rawText, receivedAt) > 0) return@withLock false
            val reference = PaymentMessageParser.reference(parsed.rawText)
            if (reference != null) {
                if (transactionDao.countWithReference(reference, receivedAt - REFERENCE_WINDOW_MS) > 0) {
                    Log.d(TAG, "Skipping repeat of UPI ref $reference ($origin)")
                    return@withLock false
                }
            } else if (origin == ORIGIN_SMS_INBOX &&
                transactionDao.countSameAmountBetween(parsed.amount, receivedAt - NEARBY_MS, receivedAt + NEARBY_MS) > 0
            ) {
                // Already heard live from the payment app (no reference to compare), so don't add it again.
                return@withLock false
            }
            transactionRepository.recordTransaction(transaction(parsed, receivedAt))
            true
        }
        if (!isNew) return false
        Log.d(TAG, "Payment ₹${parsed.amount} from '${parsed.sender}' via ${parsed.source} [$origin]")

        cloudSync.uploadTransaction(transaction(parsed, receivedAt))
        val announcementId = announcementRepository.enqueue(
            Announcement(amount = parsed.amount, sender = parsed.sender, sourceApp = parsed.source, timestamp = receivedAt)
        )
        if (speak) PaymentEventBus.emit(parsed.copy(announcementId = announcementId))

        if (settingsRepository.forwardingEnabled.first()) {
            runCatching { forwardingRepository.forward(parsed.amount, parsed.sender, receivedAt) }
                .onFailure { Log.w(TAG, "Forwarding failed", it) }
        }
        return true
    }

    private fun transaction(parsed: ParsedPayment, receivedAt: Long) = Transaction(
        amount = parsed.amount,
        sender = parsed.sender,
        sourceApp = parsed.source,
        timestamp = receivedAt,
        rawNotification = parsed.rawText
    )

    companion object {
        private const val TAG = "PaymentRecorder"
        const val ORIGIN_LIVE = "live"
        const val ORIGIN_SHADE = "notification shade"
        const val ORIGIN_SMS_INBOX = "sms inbox"
        private const val REFERENCE_WINDOW_MS = 3 * 24 * 60 * 60 * 1000L
        private const val NEARBY_MS = 3 * 60 * 1000L
    }
}
