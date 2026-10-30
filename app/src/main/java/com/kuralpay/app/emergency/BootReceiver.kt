package com.kuralpay.app.emergency

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import com.kuralpay.app.lockscreenoverlay.OverlayService
import com.kuralpay.app.summary.DailySummaryScheduler
import com.kuralpay.app.voice.tts.AnnouncementService
import com.kuralpay.app.widgets.WidgetUpdateWorker
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Restores background work after the phone restarts or KuralPay is updated in place.
 * An update keeps all data (same signer, same app ID); this only restarts what the update stopped.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var dailySummaryScheduler: DailySummaryScheduler

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> {
                WidgetUpdateWorker.schedule(context)
                dailySummaryScheduler.syncWithSettings()
                startAnnouncementServices(context)
            }
        }
    }

    /** Both broadcasts are allowed to start foreground services from the background on Android 12+. */
    private fun startAnnouncementServices(context: Context) {
        try {
            ContextCompat.startForegroundService(context, Intent(context, AnnouncementService::class.java))
            if (Settings.canDrawOverlays(context)) {
                ContextCompat.startForegroundService(context, Intent(context, OverlayService::class.java))
            }
        } catch (e: Exception) {
            Log.w("BootReceiver", "Could not restart services; they start when KuralPay is opened", e)
        }
    }
}
