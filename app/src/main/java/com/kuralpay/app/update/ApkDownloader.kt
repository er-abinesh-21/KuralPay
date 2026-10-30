package com.kuralpay.app.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

class DownloadException(message: String) : IOException(message)

/**
 * Downloads release assets over HTTPS only. Redirects are followed by hand so every hop
 * (github.com -> GitHub's asset CDN) can be checked against an allowlist.
 */
@Singleton
class ApkDownloader @Inject constructor() {

    private val http = OkHttpClient.Builder()
        .followRedirects(false)
        .followSslRedirects(false)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    /** Streams [url] into [target], reporting (bytesSoFar, totalBytes). Returns the file's SHA-256. */
    suspend fun download(url: String, target: File, expectedSize: Long, onProgress: (Long, Long) -> Unit): String =
        withContext(Dispatchers.IO) {
            val partial = File(target.parentFile, target.name + ".part")
            target.parentFile?.mkdirs()
            partial.delete()
            try {
                val response = openFollowingRedirects(url)
                response.use { res ->
                    val body = res.body ?: throw DownloadException("Empty download")
                    val total = body.contentLength().takeIf { it > 0 } ?: expectedSize
                    if (total > MAX_APK_BYTES) throw DownloadException("Update file is unexpectedly large")
                    val digest = MessageDigest.getInstance("SHA-256")
                    var done = 0L
                    body.byteStream().use { input ->
                        partial.outputStream().use { output ->
                            val buffer = ByteArray(64 * 1024)
                            while (true) {
                                currentCoroutineContext().ensureActive()
                                val read = input.read(buffer)
                                if (read < 0) break
                                output.write(buffer, 0, read)
                                digest.update(buffer, 0, read)
                                done += read
                                if (done > MAX_APK_BYTES) throw DownloadException("Update file is unexpectedly large")
                                onProgress(done, total)
                            }
                        }
                    }
                    if (expectedSize > 0 && done != expectedSize) throw DownloadException("Download was incomplete")
                    target.delete()
                    if (!partial.renameTo(target)) throw DownloadException("Could not save the update")
                    digest.digest().joinToString("") { "%02x".format(it) }
                }
            } catch (e: Exception) {
                partial.delete()
                throw e
            }
        }

    /** Small text assets such as the .sha256 file. */
    suspend fun downloadText(url: String, maxBytes: Int = 4096): String = withContext(Dispatchers.IO) {
        openFollowingRedirects(url).use { res ->
            val bytes = res.body?.byteStream()?.use { it.readNBytesCompat(maxBytes) } ?: ByteArray(0)
            String(bytes, Charsets.UTF_8)
        }
    }

    private fun openFollowingRedirects(start: String): okhttp3.Response {
        var url = start
        repeat(MAX_REDIRECTS) {
            requireAllowed(url)
            val response = http.newCall(
                Request.Builder().url(url).header("User-Agent", "KuralPay-Updater").header("Accept", "application/octet-stream").build()
            ).execute()
            if (response.isRedirect) {
                val next = response.header("Location")
                response.close()
                url = next?.let { loc -> url.toHttpUrlOrNull()?.resolve(loc)?.toString() } ?: throw DownloadException("Bad redirect")
                return@repeat
            }
            if (!response.isSuccessful) {
                response.close()
                throw DownloadException("Server returned HTTP ${response.code}")
            }
            return response
        }
        throw DownloadException("Too many redirects")
    }

    private fun requireAllowed(url: String) {
        val parsed = url.toHttpUrlOrNull() ?: throw DownloadException("Invalid download address")
        if (parsed.scheme != "https" || parsed.host.lowercase() !in ALLOWED_HOSTS) {
            throw DownloadException("Refusing to download from ${parsed.host}")
        }
    }

    private fun java.io.InputStream.readNBytesCompat(max: Int): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val buf = ByteArray(1024)
        while (out.size() < max) {
            val n = read(buf, 0, minOf(buf.size, max - out.size()))
            if (n < 0) break
            out.write(buf, 0, n)
        }
        return out.toByteArray()
    }

    private companion object {
        const val MAX_REDIRECTS = 5
        const val MAX_APK_BYTES = 200L * 1024 * 1024
        val ALLOWED_HOSTS = setOf(
            "github.com",
            "objects.githubusercontent.com",
            "release-assets.githubusercontent.com",
            "github-releases.githubusercontent.com"
        )
    }
}
