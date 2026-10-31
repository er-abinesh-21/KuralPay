package com.kuralpay.app.paymentdetection

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

/**
 * Whether the phone keeps stopping KuralPay in the background, and how to stop it doing that.
 *
 * Some makers' power managers force-stop apps every few minutes (Vivo's com.vivo.abe did so to KuralPay
 * five times in 25 minutes), which also cancels its watchdog, so nothing is heard until KuralPay is
 * opened again. Only the phone's own settings can allow KuralPay to keep running.
 */
object BackgroundHealth {

    private const val PREFS = "background_health"
    private const val KEY_ACKNOWLEDGED = "acknowledged_until"

    /** Android's own record of why KuralPay last stopped: a recent stop by the system (not an update or the person). */
    fun stoppedBySystemRecently(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return false
        val acknowledged = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(KEY_ACKNOWLEDGED, 0)
        val since = maxOf(acknowledged, System.currentTimeMillis() - 24 * 60 * 60 * 1000L)
        val exits = runCatching {
            context.getSystemService(ActivityManager::class.java).getHistoricalProcessExitReasons(context.packageName, 0, 10)
        }.getOrNull().orEmpty()
        return exits.any { it.timestamp > since && isSystemStop(it) }
    }

    private fun isSystemStop(exit: ApplicationExitInfo): Boolean {
        val description = exit.description.orEmpty()
        return when (exit.reason) {
            // "stop com.kuralpay.app due to stop by com.vivo.abe": a power manager, not the person or an update.
            ApplicationExitInfo.REASON_USER_REQUESTED -> description.contains("stop by") && !description.contains("installPackage")
            ApplicationExitInfo.REASON_OTHER -> description.contains("PeriodicCleaner") || description.contains("pressure")
            else -> false
        }
    }

    /** "Done": hide the card until the phone stops KuralPay again. */
    fun acknowledge(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putLong(KEY_ACKNOWLEDGED, System.currentTimeMillis()).apply()
    }

    fun ignoringBatteryOptimizations(context: Context): Boolean =
        context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)

    /** Android's "Allow KuralPay to always run in the background?" dialog. */
    @SuppressLint("BatteryLife")
    fun batteryExemptionIntent(context: Context): Intent =
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}"))

    fun appSettingsIntent(context: Context): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))

    /** The maker's own switches, in the words their Settings app uses. */
    fun makerSteps(): List<String> = when (Build.MANUFACTURER.lowercase()) {
        "vivo", "iqoo" -> listOf(
            "App info → Battery → turn on \"Allow high background power consumption\"",
            "i Manager → App manager → Autostart → turn on KuralPay",
            "In Recent apps, pull KuralPay down to lock it"
        )
        "xiaomi", "redmi", "poco" -> listOf(
            "App info → Battery saver → \"No restrictions\"",
            "App info → Autostart → turn on",
            "Don't clear KuralPay from Recent apps"
        )
        "oppo", "realme", "oneplus" -> listOf(
            "App info → Battery usage → allow background activity and auto launch",
            "In Recent apps, lock KuralPay"
        )
        "samsung" -> listOf(
            "App info → Battery → \"Unrestricted\"",
            "Settings → Battery → Background usage limits → remove KuralPay from sleeping apps"
        )
        else -> listOf(
            "App info → Battery → \"Unrestricted\" or \"Don't optimize\"",
            "Don't clear KuralPay from Recent apps"
        )
    }
}
