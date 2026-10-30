package com.kuralpay.app.data.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kuralpay.app.data.local.dao.AnnouncementQueueDao
import com.kuralpay.app.data.local.dao.TransactionDao
import com.kuralpay.app.data.repository.SettingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/** Deletes payment history older than the period chosen in Settings, on the phone and in the cloud. */
@HiltWorker
class RetentionWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val settings: SettingsRepository,
    private val transactionDao: TransactionDao,
    private val announcementDao: AnnouncementQueueDao,
    private val cloud: CloudSyncRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val days = settings.retentionDays.first()
        if (days <= 0) return Result.success()

        val cutoff = System.currentTimeMillis() - days * DAY_MS
        val local = transactionDao.deleteOlderThan(cutoff)
        announcementDao.deleteOlderThan(cutoff)
        val remote = try {
            cloud.deleteOlderThan(cutoff)
        } catch (e: Exception) {
            Log.w(TAG, "Cloud cleanup failed; will retry", e)
            return Result.retry()
        }
        Log.d(TAG, "Retention $days d: removed $local on phone, $remote in cloud")
        return Result.success()
    }

    companion object {
        const val DAILY = "retention_daily"
        const val NOW = "retention_now"
        private const val TAG = "RetentionWorker"
        private const val DAY_MS = 24L * 60 * 60 * 1000
    }
}
