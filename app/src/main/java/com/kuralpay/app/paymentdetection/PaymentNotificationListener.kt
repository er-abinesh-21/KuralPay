package com.kuralpay.app.paymentdetection

import android.app.Notification
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.kuralpay.app.data.repository.AnnouncementRepository
import com.kuralpay.app.data.repository.SettingsRepository
import com.kuralpay.app.data.repository.TransactionRepository
import com.kuralpay.app.data.sync.CloudSyncRepository
import com.kuralpay.app.forwarding.ForwardingRepository
import kotlinx.coroutines.flow.first
import com.kuralpay.app.domain.model.Announcement
import com.kuralpay.app.domain.model.Transaction
import com.kuralpay.app.paymentdetection.parser.PaymentParserEngine
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PaymentNotificationListener : NotificationListenerService() {

    @Inject lateinit var parserEngine: PaymentParserEngine
    @Inject lateinit var transactionRepository: TransactionRepository
    @Inject lateinit var announcementRepository: AnnouncementRepository
    @Inject lateinit var forwardingRepository: ForwardingRepository
    @Inject lateinit var cloudSync: CloudSyncRepository
    @Inject lateinit var settingsRepository: SettingsRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** SMS apps repost the same notification (new message, read state); remember what we've already counted. */
    private val recentlySeen = LinkedHashMap<String, Long>()

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName || sbn.isOngoing) return
        val notification = sbn.notification
        if (notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return

        val extras = notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val body = latestMessage(extras)
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: return
        val source = sbn.packageName

        scope.launch {
            val parsed = parserEngine.parse(source, title, body) ?: return@launch
            if (!firstTimeSeen("$source|${parsed.amount}|${body.trim()}")) return@launch
            Log.d(TAG, "Payment ₹${parsed.amount} from '${parsed.sender}' via ${parsed.source} ($source)")

            val transaction = Transaction(
                amount = parsed.amount,
                sender = parsed.sender,
                sourceApp = parsed.source,
                timestamp = sbn.postTime,
                rawNotification = parsed.rawText
            )
            transactionRepository.recordTransaction(transaction)
            cloudSync.uploadTransaction(transaction)
            val announcementId = announcementRepository.enqueue(
                Announcement(
                    amount = parsed.amount,
                    sender = parsed.sender,
                    sourceApp = parsed.source,
                    timestamp = sbn.postTime
                )
            )
            PaymentEventBus.emit(parsed.copy(announcementId = announcementId))

            if (settingsRepository.forwardingEnabled.first()) {
                runCatching { forwardingRepository.forward(parsed.amount, parsed.sender) }
                    .onFailure { Log.w(TAG, "Forwarding failed", it) }
            }
        }
    }

    /** Messaging-style notifications (SMS apps) keep each message separately; the last one is the new one. */
    @Suppress("DEPRECATION")
    private fun latestMessage(extras: Bundle): String? =
        (extras.getParcelableArray(Notification.EXTRA_MESSAGES)?.lastOrNull() as? Bundle)
            ?.getCharSequence("text")?.toString()

    @Synchronized
    private fun firstTimeSeen(key: String): Boolean {
        val now = System.currentTimeMillis()
        recentlySeen.entries.removeAll { now - it.value > DEDUPE_WINDOW_MS }
        if (key in recentlySeen) return false
        recentlySeen[key] = now
        return true
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {}

    override fun onListenerConnected() {
        Log.d(TAG, "Connected")
    }

    private companion object {
        const val TAG = "PaymentNotificationListener"
        const val DEDUPE_WINDOW_MS = 15 * 60 * 1000L
    }
}
