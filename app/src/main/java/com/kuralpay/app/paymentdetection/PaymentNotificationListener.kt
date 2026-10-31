package com.kuralpay.app.paymentdetection

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.content.ContextCompat
import com.kuralpay.app.paymentdetection.parser.PaymentParserEngine
import com.kuralpay.app.voice.tts.AnnouncementService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PaymentNotificationListener : NotificationListenerService() {

    @Inject lateinit var parserEngine: PaymentParserEngine
    @Inject lateinit var recorder: PaymentRecorder
    @Inject lateinit var missedSms: MissedSmsRecovery

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** SMS apps repost the same notification (new message, read state); remember what we've already counted. */
    private val recentlySeen = LinkedHashMap<String, Long>()

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        scope.launch { handle(sbn, recovered = false) }
    }

    /**
     * On low-memory phones Android stops KuralPay and later reconnects this listener. Payments that
     * arrived in between are still in the notification shade or the SMS inbox: record and forward them,
     * and read out only recent ones.
     */
    override fun onListenerConnected() {
        Log.d(TAG, "Connected")
        startAnnouncements(this)
        scope.launch {
            delay(RECOVERY_DELAY_MS) // let the voice engine start before anything is spoken
            val now = System.currentTimeMillis()
            val pending = runCatching { activeNotifications }.getOrNull().orEmpty()
                .filter { now - it.postTime <= RECOVERY_WINDOW_MS }
                .sortedBy { it.postTime }
            pending.forEach { handle(it, recovered = true) }
            missedSms.recover()
        }
    }

    override fun onListenerDisconnected() {
        Log.w(TAG, "Disconnected; asking Android to reconnect")
        NotificationListenerService.requestRebind(ComponentName(this, PaymentNotificationListener::class.java))
    }

    private suspend fun handle(sbn: StatusBarNotification, recovered: Boolean) {
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

        val parsed = parserEngine.parse(source, title, body) ?: return
        // Title too: GPay Business keeps the payer and time there and repeats the same body for every payment.
        if (!firstTimeSeen("$source|${parsed.amount}|${title.trim()}|${body.trim()}")) return
        val receivedAt = eventTime(sbn)
        // A payment found minutes later stays in "Replay missed" rather than being read out late.
        val late = recovered && System.currentTimeMillis() - receivedAt > ANNOUNCE_LATE_MS
        recorder.record(
            parsed,
            receivedAt = receivedAt,
            speak = !late,
            origin = if (recovered) PaymentRecorder.ORIGIN_SHADE else PaymentRecorder.ORIGIN_LIVE
        )
    }

    /**
     * When the payment happened. Apps re-post old notifications (GPay Business does when its group changes),
     * which moves postTime to "now" and once put yesterday's evening payments into today's total.
     * Notification.when is the time the app gave the event, and it stays the same across re-posts.
     */
    private fun eventTime(sbn: StatusBarNotification): Long {
        val now = System.currentTimeMillis()
        val `when` = sbn.notification.`when`
        return if (`when` in (now - EVENT_TIME_MAX_AGE_MS)..(now + 60_000)) minOf(`when`, sbn.postTime) else sbn.postTime
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

    companion object {
        private const val TAG = "PaymentNotificationListener"
        private const val DEDUPE_WINDOW_MS = 15 * 60 * 1000L
        private const val RECOVERY_WINDOW_MS = 6 * 60 * 60 * 1000L
        private const val RECOVERY_DELAY_MS = 3_000L
        private const val ANNOUNCE_LATE_MS = 5 * 60 * 1000L
        private const val EVENT_TIME_MAX_AGE_MS = 7 * 24 * 60 * 60 * 1000L

        /** Asks Android to reconnect the listener if it was dropped, e.g. after the app was stopped for memory. */
        fun ensureConnected(context: Context) {
            runCatching { NotificationListenerService.requestRebind(ComponentName(context, PaymentNotificationListener::class.java)) }
                .onFailure { Log.w(TAG, "requestRebind failed", it) }
        }

        private fun startAnnouncements(context: Context) {
            runCatching { ContextCompat.startForegroundService(context, Intent(context, AnnouncementService::class.java)) }
                .onFailure { Log.w(TAG, "Couldn't start announcements from the listener", it) }
        }
    }
}
