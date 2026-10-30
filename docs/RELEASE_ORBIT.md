# Releasing KuralPay through Release Orbit

KuralPay APKs are published to the public [Release Orbit](https://github.com/er-abinesh-21/Release-Orbit) repository. The app finds its own updates through the GitHub Releases API and installs them only after the user confirms in Android's installer.

| | |
|---|---|
| Tag | `KuralPay/vX.Y.Z` (exact prefix; other apps are ignored) |
| Title | `KuralPay - vX.Y.Z` |
| Assets | `KuralPay-vX.Y.Z.apk` and `KuralPay-vX.Y.Z.apk.sha256` |
| versionCode | `major*10000 + minor*100 + patch` (1.2.3 → 10203) |
| Signer SHA-256 | `7256fe5a11ade58e3f795516a6ac44d5abbeb3ff3d790068dbf0f87e6023ad75` (never change) |

## How releases are made

Releases are built and published from the release PC with [`scripts/release-orbit.ps1`](../scripts/release-orbit.ps1). No GitHub secrets, Actions or tokens are involved:

- **Signing** uses the keystore on the PC (`~/.android/debug.keystore`, alias `androiddebugkey`), whose certificate is the signer above. Back this file up somewhere private; if it is lost, installed apps can no longer be updated in place. Anyone who has it can sign updates, so never commit or share it.
- **Publishing** uses your own `gh` login (`gh auth status`), which needs write access to `Release-Orbit`.
- `app/google-services.json` must be present, so sign-in and cloud sync are built in.

## Publishing a version

1. Bump `versionName` and `versionCode` in `app/build.gradle.kts`, for example `1.0.1` and `10001`.
2. Add `docs/release-notes/v1.0.1.md`. Its text is shown in the app as *What's new*.
3. Commit.
4. Dry run: runs the unit tests, builds and signs, validates the package, version, signer and "not debuggable", checks the versionCode is higher than the last KuralPay release, and writes `dist/KuralPay-v1.0.1.apk` plus `.sha256`. Nothing is uploaded.
   ```powershell
   .\scripts\release-orbit.ps1 -Version 1.0.1
   ```
5. Publish: does the same, asks you to type the version to confirm, creates the release, then checks the public, unauthenticated download matches the built APK.
   ```powershell
   .\scripts\release-orbit.ps1 -Version 1.0.1 -Publish
   ```
6. Installed apps see the update within 6 hours, or immediately from *Settings → Check for updates*.

If PowerShell blocks the script, run it with `powershell -ExecutionPolicy Bypass -File scripts\release-orbit.ps1 ...`.

## What the app checks before installing

1. HTTPS only, and only from `github.com` or GitHub's asset hosts.
2. The release is stable (not a draft or prerelease), its tag is exactly `KuralPay/vX.Y.Z`, and it has exactly one `KuralPay-vX.Y.Z.apk`.
3. Size, plus the published SHA-256 (protection against corrupted downloads).
4. Package is `com.kuralpay.app`, versionCode is higher than the installed one, minSdk and ABI fit the phone.
5. **The APK is signed by the same certificate as the installed app.** This is the authenticity check; the release title, file name and checksum come from the same public page and are not trusted on their own.
6. Android's installer asks the user to confirm. Installation waits while a payment is being announced.

An in-place update keeps the database, settings, sign-in and Groq key. After the update, KuralPay restarts its announcement services itself, so payments keep being spoken. Never uninstall to fix an update problem: uninstalling deletes local payment history and settings.
