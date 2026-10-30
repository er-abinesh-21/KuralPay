package com.kuralpay.app.update

import android.content.Context
import com.kuralpay.app.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

sealed interface ReleaseFetch {
    data class Ok(val releases: List<GitHubRelease>) : ReleaseFetch
    /** GitHub's anonymous limit was hit; [resetAtEpochSeconds] says when it lifts. */
    data class RateLimited(val resetAtEpochSeconds: Long?) : ReleaseFetch
    data class Failed(val reason: String) : ReleaseFetch
}

/**
 * Reads Release Orbit's public release list anonymously. No token is ever embedded or sent.
 * The last good response is cached with its ETag, so repeat checks cost nothing when nothing changed.
 */
@Singleton
class ReleaseOrbitClient @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val cacheFile get() = File(context.filesDir, "release_orbit_cache.json")
    private val prefs get() = context.getSharedPreferences("release_orbit", Context.MODE_PRIVATE)

    suspend fun fetch(): ReleaseFetch = withContext(Dispatchers.IO) {
        try {
            var url: String? = "https://api.github.com/repos/$RELEASE_ORBIT_OWNER/$RELEASE_ORBIT_REPO/releases?per_page=100"
            val all = JSONArray()
            var page = 0
            while (url != null && page < MAX_PAGES) {
                val builder = Request.Builder()
                    .url(url)
                    .header("Accept", "application/vnd.github+json")
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .header("User-Agent", "KuralPay/${BuildConfig.VERSION_NAME} (Android)")
                // Conditional request on the first page: a 304 means our cached list is still current.
                val etag = prefs.getString(KEY_ETAG, null)
                if (page == 0 && etag != null && cacheFile.exists()) builder.header("If-None-Match", etag)

                http.newCall(builder.build()).execute().use { response ->
                    when {
                        response.code == 304 && page == 0 -> return@withContext ReleaseFetch.Ok(parse(JSONArray(cacheFile.readText())))
                        response.code == 403 || response.code == 429 -> {
                            val remaining = response.header("X-RateLimit-Remaining")
                            if (response.code == 429 || remaining == "0" || response.header("Retry-After") != null) {
                                return@withContext ReleaseFetch.RateLimited(response.header("X-RateLimit-Reset")?.toLongOrNull())
                            }
                            return@withContext ReleaseFetch.Failed("HTTP ${response.code}")
                        }
                        !response.isSuccessful -> return@withContext ReleaseFetch.Failed("HTTP ${response.code}")
                    }
                    val body = response.body?.string() ?: return@withContext ReleaseFetch.Failed("Empty response")
                    val pageArray = JSONArray(body)
                    for (i in 0 until pageArray.length()) all.put(pageArray.get(i))
                    if (page == 0) response.header("ETag")?.let { prefs.edit().putString(KEY_ETAG, it).apply() }
                    url = ReleaseSelector.nextPageUrl(response.header("Link"))
                }
                page++
            }
            cacheFile.writeText(all.toString())
            ReleaseFetch.Ok(parse(all))
        } catch (e: Exception) {
            ReleaseFetch.Failed(e.javaClass.simpleName)
        }
    }

    private fun parse(array: JSONArray): List<GitHubRelease> = (0 until array.length()).mapNotNull { i ->
        val r = array.optJSONObject(i) ?: return@mapNotNull null
        val assetsJson = r.optJSONArray("assets") ?: JSONArray()
        GitHubRelease(
            tagName = r.optString("tag_name"),
            body = r.optString("body").takeIf { it != "null" },
            draft = r.optBoolean("draft", true),
            prerelease = r.optBoolean("prerelease", true),
            assets = (0 until assetsJson.length()).mapNotNull { j ->
                val a = assetsJson.optJSONObject(j) ?: return@mapNotNull null
                GitHubAsset(
                    name = a.optString("name"),
                    size = a.optLong("size", 0),
                    downloadUrl = a.optString("browser_download_url")
                )
            }
        )
    }

    private companion object {
        const val MAX_PAGES = 10
        const val KEY_ETAG = "etag"
    }
}
