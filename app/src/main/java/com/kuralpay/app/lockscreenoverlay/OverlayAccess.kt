package com.kuralpay.app.lockscreenoverlay

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/** "Display over other apps", which the lock-screen card needs. Announcements never depend on it. */
object OverlayAccess {

    private const val PREFS = "onboarding"
    private const val KEY_SKIPPED = "overlay_skipped"

    /** Android Go (low-RAM) phones switch the setting off for every app. */
    fun supported(context: Context): Boolean =
        !context.getSystemService(ActivityManager::class.java).isLowRamDevice

    fun granted(context: Context): Boolean = Settings.canDrawOverlays(context)

    fun settingsIntent(context: Context): Intent =
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))

    /** The user chose "Skip for now" during setup; they can still allow it later from Settings. */
    fun skipped(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_SKIPPED, false)

    fun skip(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_SKIPPED, true).apply()
    }
}
