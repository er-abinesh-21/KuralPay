package com.kuralpay.app.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseSelectorTest {

    private fun apk(name: String, size: Long = 5_000_000, url: String = "https://github.com/er-abinesh-21/Release-Orbit/releases/download/x/$name") =
        GitHubAsset(name, size, url)

    private fun release(tag: String, vararg assets: GitHubAsset, draft: Boolean = false, pre: Boolean = false, body: String = "notes") =
        GitHubRelease(tag, body, draft, pre, assets.toList())

    private fun kp(v: String, draft: Boolean = false, pre: Boolean = false) =
        release("KuralPay/v$v", apk("KuralPay-v$v.apk"), apk("KuralPay-v$v.apk.sha256", 90), draft = draft, pre = pre)

    // Mirrors what Release Orbit holds today: other apps only.
    private val otherApps = listOf(
        release("Pocket-Pulse/v2.2.0", apk("PocketPulse-v2.2.0.apk"), apk("PocketPulse-v2.2.0.apk.sha256", 90)),
        release("Obscura-Vault/v1.2.0", apk("Obscura-Vault-v1.2.0.apk")),
        release("Enerza/v2.0.0", apk("Enerza-v2.0.0.apk")),
        release("Docu-Haven/v2.0.0", apk("Docu-Haven-v2.0.0.apk"))
    )

    @Test fun noKuralPayReleasesMeansNoUpdate() = assertNull(ReleaseSelector.latest(otherApps))

    @Test fun picksOnlyKuralPayAmongOtherApps() {
        val u = ReleaseSelector.latest(otherApps + kp("1.1.0"))!!
        assertEquals("KuralPay/v1.1.0", u.tag)
        assertEquals("KuralPay-v1.1.0.apk", u.apk.name)
        assertEquals("KuralPay-v1.1.0.apk.sha256", u.checksum?.name)
    }

    @Test fun picksHighestVersionNotNewestPublished() {
        // Publication order on GitHub is not version order.
        val u = ReleaseSelector.latest(listOf(kp("1.2.0"), kp("1.10.0"), kp("1.9.3")))!!
        assertEquals("1.10.0", u.version.toString())
    }

    @Test fun skipsDraftsAndPrereleases() {
        val u = ReleaseSelector.latest(listOf(kp("1.0.0"), kp("2.0.0", draft = true), kp("1.5.0", pre = true)))!!
        assertEquals("1.0.0", u.version.toString())
    }

    @Test fun ignoresLookalikePrefixes() {
        val tricky = listOf(
            release("KuralPayPro/v9.0.0", apk("KuralPay-v9.0.0.apk")),
            release("kuralpay/v9.0.0", apk("KuralPay-v9.0.0.apk")),
            release("Other/KuralPay/v9.0.0", apk("KuralPay-v9.0.0.apk"))
        )
        assertNull(ReleaseSelector.latest(tricky))
    }

    @Test fun skipsMalformedTags() {
        val bad = listOf("KuralPay/v1.0", "KuralPay/v01.0.0", "KuralPay/v1.0.0-beta", "KuralPay/vlatest", "KuralPay/v")
            .map { release(it, apk("KuralPay-v1.0.0.apk")) }
        assertNull(ReleaseSelector.latest(bad))
    }

    @Test fun skipsReleaseWithoutCorrectlyNamedApk() {
        val r = release("KuralPay/v3.0.0", apk("app-release.apk"), apk("KuralPay-v2.0.0.apk"))
        assertNull(ReleaseSelector.latest(listOf(r)))
    }

    @Test fun skipsReleaseWithDuplicateApkAssets() {
        val r = release("KuralPay/v3.0.0", apk("KuralPay-v3.0.0.apk"), apk("KuralPay-v3.0.0.apk"))
        assertNull(ReleaseSelector.latest(listOf(r)))
    }

    @Test fun skipsNonHttpsOrEmptyAssets() {
        val insecure = release("KuralPay/v3.0.0", apk("KuralPay-v3.0.0.apk", url = "http://example.com/a.apk"))
        val empty = release("KuralPay/v3.1.0", apk("KuralPay-v3.1.0.apk", size = 0))
        assertNull(ReleaseSelector.latest(listOf(insecure, empty)))
    }

    @Test fun ambiguousSameVersionFallsBackToNextBest() {
        val u = ReleaseSelector.latest(listOf(kp("2.0.0"), kp("2.0.0"), kp("1.4.0")))!!
        assertEquals("1.4.0", u.version.toString())
    }

    @Test fun checksumIsOptional() {
        val u = ReleaseSelector.latest(listOf(release("KuralPay/v1.0.1", apk("KuralPay-v1.0.1.apk"))))!!
        assertNull(u.checksum)
    }

    @Test fun newerComparison() {
        assertTrue(ReleaseSelector.isNewer(SemVer(1, 0, 1), "1.0.0"))
        assertTrue(ReleaseSelector.isNewer(SemVer(1, 10, 0), "1.9.9"))
        assertFalse(ReleaseSelector.isNewer(SemVer(1, 0, 0), "1.0.0"))
        assertFalse(ReleaseSelector.isNewer(SemVer(0, 9, 9), "1.0.0"))
        assertFalse(ReleaseSelector.isNewer(SemVer(9, 0, 0), "not-a-version"))
    }

    @Test fun nextPageFromLinkHeader() {
        val link = "<https://api.github.com/repositories/1/releases?per_page=100&page=2>; rel=\"next\", " +
            "<https://api.github.com/repositories/1/releases?per_page=100&page=3>; rel=\"last\""
        assertEquals("https://api.github.com/repositories/1/releases?per_page=100&page=2", ReleaseSelector.nextPageUrl(link))
        assertNull(ReleaseSelector.nextPageUrl("<https://api.github.com/x?page=1>; rel=\"prev\""))
        assertNull(ReleaseSelector.nextPageUrl("<https://evil.example.com/x>; rel=\"next\""))
        assertNull(ReleaseSelector.nextPageUrl(null))
    }

    @Test fun sha256FileParsing() {
        val hash = "a".repeat(64)
        assertEquals(hash, ReleaseSelector.parseSha256("$hash  KuralPay-v1.0.0.apk\n"))
        assertEquals(hash, ReleaseSelector.parseSha256(hash.uppercase()))
        assertNull(ReleaseSelector.parseSha256("not a hash"))
    }
}
