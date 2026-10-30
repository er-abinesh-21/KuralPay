# Releasing KuralPay through Release Orbit

KuralPay APKs are published to the public [Release Orbit](https://github.com/er-abinesh-21/Release-Orbit) repository. The app finds its own updates through the GitHub Releases API and installs them only after the user confirms in Android's installer.

| | |
|---|---|
| Tag | `KuralPay/vX.Y.Z` (exact prefix; other apps are ignored) |
| Title | `KuralPay - vX.Y.Z` |
| Assets | `KuralPay-vX.Y.Z.apk` and `KuralPay-vX.Y.Z.apk.sha256` |
| versionCode | `major*10000 + minor*100 + patch` (1.2.3 → 10203) |
| Signer SHA-256 | `7256fe5a11ade58e3f795516a6ac44d5abbeb3ff3d790068dbf0f87e6023ad75` (never change) |

## One-time setup (KuralPay repository settings)

**Secrets** (Settings → Secrets and variables → Actions):

| Name | Value |
|---|---|
| `KURALPAY_KEYSTORE_BASE64` | Base64 of the keystore whose certificate is the signer above |
| `KURALPAY_KEYSTORE_PASSWORD` | Keystore password |
| `KURALPAY_KEY_ALIAS` | Key alias |
| `KURALPAY_KEY_PASSWORD` | Key password |
| `KURALPAY_GOOGLE_SERVICES_JSON_BASE64` | Base64 of `app/google-services.json` (project `kuralpay-abp`) |
| `RELEASE_ORBIT_APP_PRIVATE_KEY` | Private key of the Release Orbit GitHub App (same app the other projects use) |

**Variable:** `RELEASE_ORBIT_APP_ID`, the GitHub App's ID.

**Environment:** create `release-orbit` and add yourself as a Required reviewer. The publish job waits for that approval.

The GitHub App needs only *Contents: Read and write* on `Release-Orbit`. No personal access token is used, and nothing is embedded in the app.

## Publishing a version

1. Bump `versionName` and `versionCode` in `app/build.gradle.kts`, for example `1.0.1` and `10001`.
2. Add `docs/release-notes/v1.0.1.md`. Its text is shown in the app as *What's new*.
3. Commit and push.
4. Actions → **Release to Release Orbit** → Run workflow with version `1.0.1`.
   - Leave **publish** off first: a dry run that builds, signs and validates the APK.
   - Run again with **publish** on, then approve the `release-orbit` environment.
5. The workflow checks the public download matches the built APK. Installed apps see the update within 6 hours, or immediately from *Settings → Check for updates*.

## What the app checks before installing

1. HTTPS only, and only from `github.com` or GitHub's asset hosts.
2. The release is stable (not a draft or prerelease), its tag is exactly `KuralPay/vX.Y.Z`, and it has exactly one `KuralPay-vX.Y.Z.apk`.
3. Size, plus the published SHA-256 (protection against corrupted downloads).
4. Package is `com.kuralpay.app`, versionCode is higher than the installed one, minSdk and ABI fit the phone.
5. **The APK is signed by the same certificate as the installed app.** This is the authenticity check; the release title, file name and checksum come from the same public page and are not trusted on their own.
6. Android's installer asks the user to confirm. Installation waits while a payment is being announced.

An in-place update keeps the database, settings, sign-in and Groq key. After the update, KuralPay restarts its announcement services itself, so payments keep being spoken. Never uninstall to fix an update problem: uninstalling deletes local payment history and settings.
