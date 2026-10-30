package com.kuralpay.app.emergency

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kuralpay.app.summary.DailySummaryScheduler
import com.kuralpay.app.widgets.WidgetUpdateWorker
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var dailySummaryScheduler: DailySummaryScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            WidgetUpdateWorker.schedule(context)
            dailySummaryScheduler.syncWithSettings()
        }
    }
}
