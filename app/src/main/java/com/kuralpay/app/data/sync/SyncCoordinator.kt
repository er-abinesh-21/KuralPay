package com.kuralpay.app.data.sync

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.auth.FirebaseUser
import com.kuralpay.app.account.AuthRepository
import com.kuralpay.app.data.local.SettingsPreferences
import com.kuralpay.app.data.local.dao.AnnouncementQueueDao
import com.kuralpay.app.data.local.dao.TransactionDao
import com.kuralpay.app.data.repository.SettingsRepository
import com.kuralpay.app.summary.DailySummaryScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps the phone and the cloud in step for the signed-in person and applies the retention period.
 * Started once from the Application.
 */
@Singleton
class SyncCoordinator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val auth: AuthRepository,
    private val cloud: CloudSyncRepository,
    private val prefs: SettingsPreferences,
    private val settings: SettingsRepository,
    private val transactionDao: TransactionDao,
    private val announcementDao: AnnouncementQueueDao,
    private val summaryScheduler: DailySummaryScheduler
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun start() {
        scope.launch { auth.user.collectLatest { user -> if (user != null) onSignedIn(user) } }
        scope.launch { watchRetention() }
    }

    @OptIn(FlowPreview::class)
    private suspend fun onSignedIn(user: FirebaseUser) {
        try {
            val previous = prefs.syncedUid()
            if (previous != user.uid) {
                if (previous != null) {
                    // A different person is using this phone now; never mix their payments with the last person's.
                    transactionDao.deleteAll()
                    announcementDao.deleteAll()
                }
                cloud.fetchSettings(user.uid)?.let { prefs.applySynced(it) }
                cloud.restoreAndBackfill(user.uid)
                prefs.setSyncedUid(user.uid)
                summaryScheduler.syncWithSettings(replace = true)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Initial sync failed; will retry next start", e)
        }

        // From here on, every settings change is backed up a moment after it settles.
        prefs.syncable.distinctUntilChanged().debounce(2_000).collect { cloud.uploadSettings(it) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun watchRetention() {
        val work = WorkManager.getInstance(context)
        val needsNetwork = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        settings.retentionDays.distinctUntilChanged().collectLatest { days ->
            if (days <= 0) {
                work.cancelUniqueWork(RetentionWorker.DAILY)
                return@collectLatest
            }
            work.enqueueUniquePeriodicWork(
                RetentionWorker.DAILY,
                ExistingPeriodicWorkPolicy.UPDATE,
                PeriodicWorkRequestBuilder<RetentionWorker>(24, TimeUnit.HOURS).setConstraints(needsNetwork).build()
            )
            // Apply the new period right away instead of waiting up to a day.
            work.enqueueUniqueWork(
                RetentionWorker.NOW,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<RetentionWorker>().build()
            )
        }
    }

    private companion object {
        const val TAG = "SyncCoordinator"
    }
}
