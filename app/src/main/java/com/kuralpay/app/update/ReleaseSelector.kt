package com.kuralpay.app.update

/** Release Orbit hosts several apps; KuralPay only ever looks at tags with this exact prefix. */
const val RELEASE_ORBIT_OWNER = "er-abinesh-21"
const val RELEASE_ORBIT_REPO = "Release-Orbit"
const val KURALPAY_TAG_PREFIX = "KuralPay/v"

fun kuralPayApkName(version: SemVer) = "KuralPay-v$version.apk"

data class GitHubAsset(
    val name: String,
    val size: Long,
    val downloadUrl: String
)

data class GitHubRelease(
    val tagName: String,
    val body: String?,
    val draft: Boolean,
    val prerelease: Boolean,
    val assets: List<GitHubAsset>
)

/** Strict X.Y.Z, no leading zeros, no suffixes. Anything else is ignored rather than guessed at. */
data class SemVer(val major: Int, val minor: Int, val patch: Int) : Comparable<SemVer> {
    override fun compareTo(other: SemVer): Int =
        compareValuesBy(this, other, SemVer::major, SemVer::minor, SemVer::patch)

    override fun toString() = "$major.$minor.$patch"

    companion object {
        private val PATTERN = Regex("""^(0|[1-9]\d{0,5})\.(0|[1-9]\d{0,5})\.(0|[1-9]\d{0,5})$""")

        fun parse(text: String): SemVer? = PATTERN.matchEntire(text.trim())?.destructured?.let { (a, b, c) ->
            SemVer(a.toInt(), b.toInt(), c.toInt())
        }
    }
}

data class AvailableUpdate(
    val version: SemVer,
    val tag: String,
    val notes: String,
    val apk: GitHubAsset,
    val checksum: GitHubAsset?
)

object ReleaseSelector {

    /**
     * The highest stable KuralPay release that has exactly one correctly named APK.
     * Release metadata is untrusted: anything malformed or ambiguous is skipped, never "fixed up".
     */
    fun latest(releases: List<GitHubRelease>): AvailableUpdate? {
        val candidates = releases
            .asSequence()
            .filter { !it.draft && !it.prerelease }
            .filter { it.tagName.startsWith(KURALPAY_TAG_PREFIX) }
            .mapNotNull { release ->
                val version = SemVer.parse(release.tagName.removePrefix(KURALPAY_TAG_PREFIX)) ?: return@mapNotNull null
                val apkName = kuralPayApkName(version)
                val apk = release.assets.filter { it.name == apkName }.singleOrNull() ?: return@mapNotNull null
                if (!apk.downloadUrl.startsWith("https://") || apk.size <= 0) return@mapNotNull null
                val checksum = release.assets.filter { it.name == "$apkName.sha256" }.singleOrNull()
                AvailableUpdate(version, release.tagName, release.body.orEmpty().trim(), apk, checksum)
            }
            .toList()

        // Two releases claiming the same version is ambiguous; trust neither.
        val ambiguous = candidates.groupBy { it.version }.filterValues { it.size > 1 }.keys
        return candidates.filter { it.version !in ambiguous }.maxByOrNull { it.version }
    }

    /** True only when [candidate] is strictly newer than what is installed. Unparseable installed versions never upgrade. */
    fun isNewer(candidate: SemVer, installedVersionName: String): Boolean {
        val installed = SemVer.parse(installedVersionName) ?: return false
        return candidate > installed
    }

    /** The rel="next" URL from a GitHub Link header, only if it stays on the GitHub API. */
    fun nextPageUrl(linkHeader: String?): String? {
        if (linkHeader.isNullOrBlank()) return null
        return linkHeader.split(',')
            .map { it.trim() }
            .firstOrNull { it.endsWith("rel=\"next\"") }
            ?.substringAfter('<')?.substringBefore('>')
            ?.takeIf { it.startsWith("https://api.github.com/") }
    }

    /** First 64-hex-character token of a .sha256 file ("<hash>  <filename>"). */
    fun parseSha256(text: String): String? =
        Regex("""\b([0-9a-fA-F]{64})\b""").find(text)?.groupValues?.get(1)?.lowercase()
}
