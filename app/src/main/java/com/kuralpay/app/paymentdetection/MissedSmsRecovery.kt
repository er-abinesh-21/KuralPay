package com.kuralpay.app.paymentdetection

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat
import com.kuralpay.app.paymentdetection.parser.PaymentParserEngine
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bank credit SMS that arrived while Android had KuralPay stopped (common on 2 GB phones) are still in
 * the SMS inbox even after their notification is gone. With the optional SMS permission, this finds
 * them, records them under "Replay missed" and forwards them. Only reads messages; never sends or deletes.
 */
@Singleton
class MissedSmsRecovery @Inject constructor(
    @ApplicationContext private val context: Context,
    private val parserEngine: PaymentParserEngine,
    private val recorder: PaymentRecorder
) {
    private val prefs get() = context.getSharedPreferences("sms_recovery", Context.MODE_PRIVATE)
    private val running = Mutex()

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED

    /** Scans inbox messages newer than the last scan (the past day on the first run). Returns how many payments were added. */
    suspend fun recover(): Int {
        if (!hasPermission()) return 0
        if (!running.tryLock()) return 0
        try {
            return withContext(Dispatchers.IO) { scan() }
        } finally {
            running.unlock()
        }
    }

    private suspend fun scan(): Int {
        val now = System.currentTimeMillis()
        val since = prefs.getLong(KEY_SCANNED_UNTIL, now - FIRST_RUN_WINDOW_MS)
        var newest = since
        var added = 0
        val messages = mutableListOf<Triple<String, String, Long>>()
        runCatching {
            context.contentResolver.query(
                Uri.parse("content://sms/inbox"),
                arrayOf("address", "body", "date"),
                "date > ?",
                arrayOf(since.toString()),
                "date ASC"
            )?.use { c ->
                val address = c.getColumnIndexOrThrow("address")
                val body = c.getColumnIndexOrThrow("body")
                val date = c.getColumnIndexOrThrow("date")
                while (c.moveToNext()) {
                    messages += Triple(c.getString(address).orEmpty(), c.getString(body).orEmpty(), c.getLong(date))
                }
            }
        }.onFailure { Log.w(TAG, "SMS inbox not readable", it); return 0 }

        for ((address, body, date) in messages) {
            // Messages from the last moments belong to the live listener, which reads them out; check them next time.
            if (now - date < LIVE_GRACE_MS) break
            newest = maxOf(newest, date)
            val parsed = parserEngine.parse(SMS_SOURCE, address, body) ?: continue
            // Too late to announce: it goes to "Replay missed", is listed on Home, and is forwarded.
            if (recorder.record(parsed, receivedAt = date, speak = false, origin = PaymentRecorder.ORIGIN_SMS_INBOX)) added++
        }
        prefs.edit().putLong(KEY_SCANNED_UNTIL, newest).apply()
        if (added > 0) Log.d(TAG, "Recovered $added missed payment(s) from the SMS inbox")
        return added
    }

    private companion object {
        const val TAG = "MissedSmsRecovery"
        const val KEY_SCANNED_UNTIL = "scanned_until"
        const val FIRST_RUN_WINDOW_MS = 24 * 60 * 60 * 1000L
        const val LIVE_GRACE_MS = 2 * 60 * 1000L
        /** Any SMS app package: tells the parser these are SMS, so credits count as bank transfers. */
        const val SMS_SOURCE = "com.google.android.apps.messaging"
    }
}
