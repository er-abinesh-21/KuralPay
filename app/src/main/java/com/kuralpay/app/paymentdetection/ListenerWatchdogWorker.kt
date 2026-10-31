package com.kuralpay.app.paymentdetection

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.kuralpay.app.voice.tts.AnnouncementService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Low-memory phones (Android Go, 2 GB) stop KuralPay often, and Android doesn't always reconnect the
 * notification listener afterwards. Every 15 minutes this wakes the app, reconnects the listener,
 * restarts announcements and picks up bank SMS that arrived while it was stopped.
 */
@HiltWorker
class ListenerWatchdogWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val missedSms: MissedSmsRecovery
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        PaymentNotificationListener.ensureConnected(applicationContext)
        runCatching {
            ContextCompat.startForegroundService(applicationContext, Intent(applicationContext, AnnouncementService::class.java))
        }.onFailure { Log.d(TAG, "Announcements not restarted from background: ${it.javaClass.simpleName}") }
        runCatching { missedSms.recover() }
        return Result.success()
    }

    companion object {
        private const val TAG = "ListenerWatchdog"

        fun schedule(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "listener_watchdog",
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<ListenerWatchdogWorker>(15, TimeUnit.MINUTES).build()
            )
        }
    }
}
