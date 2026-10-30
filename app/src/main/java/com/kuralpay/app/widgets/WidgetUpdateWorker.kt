package com.kuralpay.app.widgets

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.kuralpay.app.data.repository.TransactionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.concurrent.TimeUnit

@HiltWorker
class WidgetUpdateWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val transactionRepository: TransactionRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val startOfDay = getStartOfDay()
        val endOfDay = startOfDay + 86400000

        val totalValue = transactionRepository.getTodayTotal(startOfDay, endOfDay).first()
        val countValue = transactionRepository.getTodayCount(startOfDay, endOfDay).first()
        val last = transactionRepository.getLastTransaction().first()
        val lastAmount = last?.amount ?: 0.0
        val lastSender = last?.sender ?: ""

        WidgetDataProvider.updateWidgetData(applicationContext, totalValue, countValue, lastAmount, lastSender)
        SmallPaymentWidget().updateAll(applicationContext)
        MediumPaymentWidget().updateAll(applicationContext)
        LargePaymentWidget().updateAll(applicationContext)

        return Result.success()
    }

    private fun getStartOfDay(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    companion object {
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(
                15, TimeUnit.MINUTES
            ).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "widget_update",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
