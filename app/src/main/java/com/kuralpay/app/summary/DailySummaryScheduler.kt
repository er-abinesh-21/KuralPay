package com.kuralpay.app.summary

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.kuralpay.app.data.repository.SettingsRepository
import com.kuralpay.app.domain.model.SummarySlot
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DailySummaryScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Repeats every 24 hours from the next occurrence of [minutesOfDay].
     * [replace] restarts the schedule (use after the time changes); otherwise an existing schedule is kept.
     */
    fun schedule(slot: SummarySlot, minutesOfDay: Int, replace: Boolean) {
        val request = PeriodicWorkRequestBuilder<DailySummaryWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delayUntil(minutesOfDay), TimeUnit.MILLISECONDS)
            .addTag(slot.tag)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            slot.workName,
            if (replace) ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE else ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun cancel(slot: SummarySlot) {
        WorkManager.getInstance(context).cancelUniqueWork(slot.workName)
    }

    /** Brings the schedule in line with Settings: on app start, after boot. */
    fun syncWithSettings(replace: Boolean = false) {
        scope.launch {
            SummarySlot.entries.forEach { slot ->
                // Older builds queued a one-time job under the bare tag; it never repeated.
                WorkManager.getInstance(context).cancelUniqueWork(slot.tag)
                if (settingsRepository.summaryEnabled(slot).first()) {
                    schedule(slot, settingsRepository.summaryTime(slot).first(), replace)
                } else {
                    cancel(slot)
                }
            }
        }
    }

    private val SummarySlot.workName get() = "${tag}_daily"

    private fun delayUntil(minutesOfDay: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, minutesOfDay / 60)
            set(Calendar.MINUTE, minutesOfDay % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (!target.after(now)) target.add(Calendar.DAY_OF_YEAR, 1)
        return target.timeInMillis - now.timeInMillis
    }
}
