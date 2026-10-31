package com.kuralpay.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.kuralpay.app.data.sync.SyncCoordinator
import com.kuralpay.app.paymentdetection.ListenerWatchdogWorker
import com.kuralpay.app.update.AutoUpdateWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class KuralPayApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var syncCoordinator: SyncCoordinator

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        syncCoordinator.start()
        ListenerWatchdogWorker.schedule(this)
        AutoUpdateWorker.schedule(this)
    }
}
