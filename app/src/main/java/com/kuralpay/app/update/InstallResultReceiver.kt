package com.kuralpay.app.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Receives the outcome of an update committed by [UpdateManager]. Not exported: only PackageInstaller's PendingIntent reaches it. */
@AndroidEntryPoint
class InstallResultReceiver : BroadcastReceiver() {

    @Inject lateinit var updateManager: UpdateManager

    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
        val confirm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
        } else {
            @Suppress("DEPRECATION") intent.getParcelableExtra(Intent.EXTRA_INTENT)
        }
        updateManager.onInstallResult(status, message, confirm)
    }
}
