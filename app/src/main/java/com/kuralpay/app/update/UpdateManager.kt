package com.kuralpay.app.update

import android.app.ActivityManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.pm.PackageInstaller
import android.os.Build
import androidx.core.app.NotificationCompat
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import com.kuralpay.app.BuildConfig
import com.kuralpay.app.NotificationHelper
import com.kuralpay.app.voice.tts.PaymentTtsEngine
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.DateFormat
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val update: AvailableUpdate) : UpdateState
    data class Downloading(val update: AvailableUpdate, val bytes: Long, val total: Long) : UpdateState
    data class Verifying(val update: AvailableUpdate) : UpdateState
    data class NeedsInstallPermission(val update: AvailableUpdate, val file: File) : UpdateState
    /** [waitingForAnnouncement] is true while a payment is being spoken; installation starts right after. */
    data class ReadyToInstall(val update: AvailableUpdate, val file: File, val waitingForAnnouncement: Boolean = false) : UpdateState
    data class Installing(val update: AvailableUpdate) : UpdateState
    data class Failed(val message: String, val update: AvailableUpdate?) : UpdateState
}

/**
 * Finds, downloads, verifies and installs KuralPay updates from Release Orbit.
 *
 * Every APK is verified (size, SHA-256, package, version, signing certificate) before it is installed.
 * Installing goes through a PackageInstaller session that asks Android for no confirmation: allowed
 * because KuralPay updates itself and declares UPDATE_PACKAGES_WITHOUT_USER_ACTION. When Android still
 * wants the person to confirm, they are asked (directly, or through a notification in the background).
 */
@Singleton
class UpdateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val client: ReleaseOrbitClient,
    private val downloader: ApkDownloader,
    private val verifier: ApkVerifier,
    private val tts: PaymentTtsEngine
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val prefs get() = context.getSharedPreferences("release_orbit", Context.MODE_PRIVATE)
    private val updatesDir get() = File(context.cacheDir, "updates")

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    /** Whether the update sheet should be on screen. */
    private val _sheetVisible = MutableStateFlow(false)
    val sheetVisible: StateFlow<Boolean> = _sheetVisible.asStateFlow()

    /** An automatic check found an update; shown when the person is on Home and nothing is being announced. */
    private val _autoPromptPending = MutableStateFlow(false)
    val autoPromptPending: StateFlow<Boolean> = _autoPromptPending.asStateFlow()

    /** "Install updates automatically" in Settings; on unless the person turns it off. */
    private val _autoInstall = MutableStateFlow(true)
    val autoInstall: StateFlow<Boolean> = _autoInstall.asStateFlow()

    init {
        _autoInstall.value = prefs.getBoolean(KEY_AUTO_INSTALL, true)
    }

    fun setAutoInstall(on: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_INSTALL, on).apply()
        _autoInstall.value = on
    }

    private var dismissedThisSession: SemVer? = null
    private var work: Job? = null

    /**
     * Read from Android, not BuildConfig: BuildConfig.VERSION_NAME is a compile-time constant copied into
     * this class, and an incremental build once left 1.2.0 thinking it was 1.0.0 (offering itself as an update).
     */
    val installedVersion: String by lazy {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull()
            ?: BuildConfig.VERSION_NAME
    }

    /** Quiet background check on launch, at most once per [AUTO_INTERVAL_MS]. Never blocks startup. */
    fun checkOnLaunch() {
        cleanupLeftovers()
        val last = prefs.getLong(KEY_LAST_AUTO, 0)
        if (System.currentTimeMillis() - last < AUTO_INTERVAL_MS) return
        scope.launch { check(manual = false) }
    }

    /** "Check for updates" from Settings or About. */
    fun checkNow() {
        if (_state.value is UpdateState.Downloading || _state.value is UpdateState.Verifying) {
            _sheetVisible.value = true
            return
        }
        _sheetVisible.value = true
        scope.launch { check(manual = true) }
    }

    private suspend fun check(manual: Boolean) {
        if (manual) _state.value = UpdateState.Checking
        val result = client.fetch()
        if (!manual) prefs.edit().putLong(KEY_LAST_AUTO, System.currentTimeMillis()).apply()
        val next = when (result) {
            is ReleaseFetch.Ok -> {
                val update = ReleaseSelector.latest(result.releases)
                if (update != null && ReleaseSelector.isNewer(update.version, installedVersion)) UpdateState.Available(update)
                else UpdateState.UpToDate
            }
            is ReleaseFetch.RateLimited -> UpdateState.Failed(
                result.resetAtEpochSeconds?.let {
                    "GitHub is limiting update checks right now. Try again after ${DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(it * 1000))}."
                } ?: "GitHub is limiting update checks right now. Try again in a little while.",
                null
            )
            is ReleaseFetch.Failed -> UpdateState.Failed("Couldn't reach GitHub. Check your internet connection and try again.", null)
        }
        if (!manual) {
            // A quiet check only ever surfaces an update, never errors or "up to date".
            if (next is UpdateState.Available && next.update.version != dismissedThisSession && !busyWithUpdate()) {
                _state.value = next
                _autoPromptPending.value = true
            }
            return
        }
        _state.value = next
    }

    /** Called by the UI when it is a safe moment (Home, no announcement) to show a pending automatic prompt. */
    fun showAutoPrompt() {
        if (_autoPromptPending.value && !tts.isBusy) {
            _autoPromptPending.value = false
            _sheetVisible.value = true
        }
    }

    fun later() {
        (_state.value as? UpdateState.Available)?.let { dismissedThisSession = it.update.version }
        _autoPromptPending.value = false
        _sheetVisible.value = false
    }

    fun hideSheet() {
        _sheetVisible.value = false
        if (_state.value is UpdateState.Available) later()
    }

    fun download() {
        val update = when (val s = _state.value) {
            is UpdateState.Available -> s.update
            is UpdateState.Failed -> s.update ?: return
            else -> return
        }
        work?.cancel()
        work = scope.launch { downloadAndVerify(update) }
    }

    /** Downloads and verifies [update]; on success the state is ReadyToInstall or NeedsInstallPermission. */
    private suspend fun downloadAndVerify(update: AvailableUpdate): File? {
        val target = File(updatesDir, kuralPayApkName(update.version))
        try {
            _state.value = UpdateState.Downloading(update, 0, update.apk.size)
            val expectedSha = update.checksum?.let { runCatching { ReleaseSelector.parseSha256(downloader.downloadText(it.downloadUrl)) }.getOrNull() }
            val actualSha = downloader.download(update.apk.downloadUrl, target, update.apk.size) { done, total ->
                _state.value = UpdateState.Downloading(update, done, total)
            }
            _state.value = UpdateState.Verifying(update)
            return when (val check = verifier.verify(target, update.apk.size, expectedSha, actualSha)) {
                is ApkCheck.Rejected -> {
                    target.delete()
                    _state.value = UpdateState.Failed("${check.reason}. The update was not installed.", update)
                    null
                }
                is ApkCheck.Accepted -> {
                    _state.value =
                        if (canRequestInstalls()) UpdateState.ReadyToInstall(update, target)
                        else UpdateState.NeedsInstallPermission(update, target)
                    target
                }
            }
        } catch (e: CancellationException) {
            target.delete()
            _state.value = UpdateState.Available(update)
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Update download failed: ${e.javaClass.simpleName}")
            target.delete()
            _state.value = UpdateState.Failed("The download didn't finish. Check your connection and try again.", update)
            return null
        }
    }

    fun cancelDownload() {
        work?.cancel()
    }

    /** Opens the system screen where the person allows KuralPay to install updates. */
    fun openInstallPermission(activityContext: Context) {
        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
        runCatching { activityContext.startActivity(intent) }
    }

    /** Re-checks the permission after the person returns from system settings. */
    fun refreshInstallPermission() {
        val s = _state.value
        if (s is UpdateState.NeedsInstallPermission && canRequestInstalls()) {
            _state.value = UpdateState.ReadyToInstall(s.update, s.file)
        }
    }

    /** Waits for any announcement in progress to finish, then installs (with no confirmation when Android allows it). */
    fun install(activityContext: Context) {
        val s = _state.value as? UpdateState.ReadyToInstall ?: return
        work?.cancel()
        work = scope.launch { installWhenQuiet(s) }
    }

    private suspend fun installWhenQuiet(ready: UpdateState.ReadyToInstall) {
        while (tts.isBusy) {
            _state.value = ready.copy(waitingForAnnouncement = true)
            delay(1_000)
        }
        if (!ready.file.isFile) {
            _state.value = UpdateState.Failed("The downloaded update is gone. Download it again.", ready.update)
            return
        }
        _state.value = UpdateState.Installing(ready.update)
        try {
            commitSession(ready.file)
        } catch (e: Exception) {
            Log.w(TAG, "Install session failed: ${e.javaClass.simpleName}")
            _state.value = UpdateState.Failed("The update couldn't be installed. Try again.", ready.update)
        }
    }

    /**
     * Automatic update, run by [AutoUpdateWorker]: check, download, verify and install while KuralPay
     * isn't on screen and nothing is being announced. If KuralPay is open, it waits for the next run.
     */
    suspend fun autoUpdate() {
        if (!_autoInstall.value || busyWithUpdate() || isInForeground()) return
        val result = client.fetch()
        prefs.edit().putLong(KEY_LAST_AUTO, System.currentTimeMillis()).apply()
        val update = (result as? ReleaseFetch.Ok)?.let { ReleaseSelector.latest(it.releases) } ?: return
        if (!ReleaseSelector.isNewer(update.version, installedVersion)) return
        Log.d(TAG, "Auto-updating to ${update.version}")
        downloadAndVerify(update) ?: return
        when (val s = _state.value) {
            is UpdateState.ReadyToInstall -> if (isInForeground()) _autoPromptPending.value = true else installWhenQuiet(s)
            is UpdateState.NeedsInstallPermission -> notifyNeedsAction(
                "KuralPay ${update.version} is ready",
                "Allow KuralPay to install updates, then open KuralPay to finish.",
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
            )
            else -> Unit
        }
    }

    /** Writes the verified APK into a PackageInstaller session and commits it; the result comes to [InstallResultReceiver]. */
    private suspend fun commitSession(apk: File) = withContext(Dispatchers.IO) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(context.packageName)
            setSize(apk.length())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) setPackageSource(PackageInstaller.PACKAGE_SOURCE_DOWNLOADED_FILE)
        }
        val sessionId = installer.createSession(params)
        installer.openSession(sessionId).use { session ->
            session.openWrite("base.apk", 0, apk.length()).use { out ->
                apk.inputStream().use { it.copyTo(out) }
                session.fsync(out)
            }
            val callback = PendingIntent.getBroadcast(
                context,
                sessionId,
                Intent(context, InstallResultReceiver::class.java).setPackage(context.packageName),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            session.commit(callback.intentSender)
        }
    }

    /** Result of a commit. On success Android replaces KuralPay and restarts its services (MY_PACKAGE_REPLACED). */
    fun onInstallResult(status: Int, message: String?, confirm: Intent?) {
        val update = (_state.value as? UpdateState.Installing)?.update
        when (status) {
            PackageInstaller.STATUS_SUCCESS -> Log.d(TAG, "Update installed")
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val intent = confirm?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) ?: return
                // Android wants a confirmation: show it now if KuralPay is open, otherwise as a notification.
                if (isInForeground() && runCatching { context.startActivity(intent) }.isSuccess) return
                notifyNeedsAction("KuralPay update ready", "Tap to finish installing the update.", intent)
            }
            PackageInstaller.STATUS_FAILURE_ABORTED -> _state.value = UpdateState.Failed("The update was cancelled.", update)
            else -> {
                Log.w(TAG, "Install failed: status $status ($message)")
                _state.value = UpdateState.Failed("Android couldn't install the update. Try again later.", update)
            }
        }
    }

    private fun notifyNeedsAction(title: String, text: String, intent: Intent) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val tap = PendingIntent.getActivity(
            context, NOTIFY_ID, intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, NotificationHelper.MISSED_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(tap)
            .setAutoCancel(true)
            .build()
        runCatching { manager.notify(NOTIFY_ID, notification) }
    }

    private fun isInForeground(): Boolean {
        val info = ActivityManager.RunningAppProcessInfo()
        ActivityManager.getMyMemoryState(info)
        return info.importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
    }

    private fun canRequestInstalls() = context.packageManager.canRequestPackageInstalls()

    private fun busyWithUpdate() = _state.value.let {
        it is UpdateState.Downloading || it is UpdateState.Verifying || it is UpdateState.ReadyToInstall ||
            it is UpdateState.NeedsInstallPermission || it is UpdateState.Installing
    }

    /** Old downloads are useless once this version is running; remove them so they never sit in storage. */
    private fun cleanupLeftovers() {
        if (!busyWithUpdate()) updatesDir.listFiles()?.forEach { it.delete() }
    }

    private companion object {
        const val TAG = "UpdateManager"
        const val KEY_LAST_AUTO = "last_auto_check"
        const val KEY_AUTO_INSTALL = "auto_install"
        const val NOTIFY_ID = 4107
        const val AUTO_INTERVAL_MS = 6 * 60 * 60 * 1000L
    }
}
