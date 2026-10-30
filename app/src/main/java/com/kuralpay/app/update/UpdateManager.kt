package com.kuralpay.app.update

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.kuralpay.app.BuildConfig
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
    data class Failed(val message: String, val update: AvailableUpdate?) : UpdateState
}

/**
 * Finds, downloads, verifies and hands KuralPay updates from Release Orbit to Android's installer.
 * Installing always needs the person's confirmation in the system dialog; nothing is installed silently.
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

    private var dismissedThisSession: SemVer? = null
    private var work: Job? = null

    val installedVersion: String get() = BuildConfig.VERSION_NAME

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
        work = scope.launch {
            val target = File(updatesDir, kuralPayApkName(update.version))
            try {
                _state.value = UpdateState.Downloading(update, 0, update.apk.size)
                val expectedSha = update.checksum?.let { runCatching { ReleaseSelector.parseSha256(downloader.downloadText(it.downloadUrl)) }.getOrNull() }
                val actualSha = downloader.download(update.apk.downloadUrl, target, update.apk.size) { done, total ->
                    _state.value = UpdateState.Downloading(update, done, total)
                }
                _state.value = UpdateState.Verifying(update)
                when (val check = verifier.verify(target, update.apk.size, expectedSha, actualSha)) {
                    is ApkCheck.Rejected -> {
                        target.delete()
                        _state.value = UpdateState.Failed("${check.reason}. The update was not installed.", update)
                    }
                    is ApkCheck.Accepted -> _state.value =
                        if (canRequestInstalls()) UpdateState.ReadyToInstall(update, target)
                        else UpdateState.NeedsInstallPermission(update, target)
                }
            } catch (e: CancellationException) {
                target.delete()
                _state.value = UpdateState.Available(update)
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Update download failed: ${e.javaClass.simpleName}")
                target.delete()
                _state.value = UpdateState.Failed("The download didn't finish. Check your connection and try again.", update)
            }
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

    /** Waits for any announcement in progress to finish, then opens Android's installer for confirmation. */
    fun install(activityContext: Context) {
        val s = _state.value as? UpdateState.ReadyToInstall ?: return
        work?.cancel()
        work = scope.launch {
            while (tts.isBusy) {
                _state.value = s.copy(waitingForAnnouncement = true)
                delay(1_000)
            }
            _state.value = s.copy(waitingForAnnouncement = false)
            if (!s.file.isFile) {
                _state.value = UpdateState.Failed("The downloaded update is gone. Download it again.", s.update)
                return@launch
            }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", s.file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                activityContext.startActivity(intent)
            } catch (_: ActivityNotFoundException) {
                _state.value = UpdateState.Failed("This phone's installer couldn't be opened.", s.update)
            }
        }
    }

    private fun canRequestInstalls() = context.packageManager.canRequestPackageInstalls()

    private fun busyWithUpdate() = _state.value.let {
        it is UpdateState.Downloading || it is UpdateState.Verifying || it is UpdateState.ReadyToInstall || it is UpdateState.NeedsInstallPermission
    }

    /** Old downloads are useless once this version is running; remove them so they never sit in storage. */
    private fun cleanupLeftovers() {
        if (!busyWithUpdate()) updatesDir.listFiles()?.forEach { it.delete() }
    }

    private companion object {
        const val TAG = "UpdateManager"
        const val KEY_LAST_AUTO = "last_auto_check"
        const val AUTO_INTERVAL_MS = 6 * 60 * 60 * 1000L
    }
}
