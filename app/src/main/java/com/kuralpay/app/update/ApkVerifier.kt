package com.kuralpay.app.update

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipFile
import javax.inject.Inject
import javax.inject.Singleton

sealed interface ApkCheck {
    data class Accepted(val versionCode: Long, val versionName: String?) : ApkCheck
    data class Rejected(val reason: String) : ApkCheck
}

/**
 * Decides whether a downloaded APK may be offered to Android's installer. The deciding check is the
 * signer: it must match the KuralPay already installed. Release names, file names and the published
 * checksum come from the same public release and are not proof of authenticity on their own.
 */
@Singleton
class ApkVerifier @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun verify(apk: File, expectedSize: Long, expectedSha256: String?, actualSha256: String): ApkCheck {
        if (!apk.isFile) return ApkCheck.Rejected("The update file is missing")
        if (expectedSize > 0 && apk.length() != expectedSize) return ApkCheck.Rejected("The update file is incomplete")
        if (expectedSha256 != null && !expectedSha256.equals(actualSha256, ignoreCase = true)) {
            return ApkCheck.Rejected("The update file is damaged (checksum mismatch)")
        }

        val pm = context.packageManager
        val candidate = archiveInfo(pm, apk) ?: return ApkCheck.Rejected("The update file is not a valid Android app")
        val installed = installedInfo(pm) ?: return ApkCheck.Rejected("Could not read the installed app")

        if (candidate.packageName != context.packageName) return ApkCheck.Rejected("The update is for a different app")

        val candidateCode = candidate.longVersionCode
        if (candidateCode <= installed.longVersionCode) return ApkCheck.Rejected("The update is not newer than this version")

        val minSdk = candidate.applicationInfo?.minSdkVersion ?: 0
        if (minSdk > Build.VERSION.SDK_INT) return ApkCheck.Rejected("This update needs a newer version of Android")

        if (!abiCompatible(apk)) return ApkCheck.Rejected("This update doesn't support this phone's processor")

        if (!sameSigner(candidate, installed)) return ApkCheck.Rejected("The update isn't signed by KuralPay")

        return ApkCheck.Accepted(candidateCode, candidate.versionName)
    }

    private fun archiveInfo(pm: PackageManager, apk: File): PackageInfo? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageArchiveInfo(apk.absolutePath, PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong()))
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageArchiveInfo(apk.absolutePath, PackageManager.GET_SIGNING_CERTIFICATES)
        }

    private fun installedInfo(pm: PackageManager): PackageInfo? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong()))
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        }
    }.getOrNull()

    /** Same signing certificate(s), or a rotated key whose history includes the installed signer. */
    private fun sameSigner(candidate: PackageInfo, installed: PackageInfo): Boolean {
        val cand = candidate.signingInfo ?: return false
        val inst = installed.signingInfo ?: return false
        val installedSigners = inst.apkContentsSigners.map { sha256(it.toByteArray()) }.toSet()
        if (installedSigners.isEmpty()) return false
        val candidateSigners = cand.apkContentsSigners.map { sha256(it.toByteArray()) }.toSet()
        if (candidateSigners == installedSigners) return true
        if (cand.hasMultipleSigners()) return false
        val history = cand.signingCertificateHistory?.map { sha256(it.toByteArray()) }.orEmpty()
        return installedSigners.size == 1 && installedSigners.first() in history
    }

    /** An APK without native code runs everywhere; one with native code must include an ABI this phone supports. */
    private fun abiCompatible(apk: File): Boolean = runCatching {
        ZipFile(apk).use { zip ->
            val abis = zip.entries().asSequence()
                .map { it.name }
                .filter { it.startsWith("lib/") && it.count { c -> c == '/' } >= 2 }
                .map { it.split('/')[1] }
                .toSet()
            abis.isEmpty() || abis.any { it in Build.SUPPORTED_ABIS }
        }
    }.getOrDefault(false)

    private fun sha256(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
}
