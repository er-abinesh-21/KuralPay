# Release Orbit: KuralPay 1.0.0

| Field | Value |
|---|---|
| Repository | `er-abinesh-21/Release-Orbit` |
| Tag | `KuralPay/v1.0.0` |
| Target | `main` |
| Title | `KuralPay - v1.0.0` |
| Assets | `KuralPay-v1.0.0.apk`, `KuralPay-v1.0.0.apk.sha256` |
| Pre-release | No |
| versionName / versionCode | `1.0.0` / `10000` |
| Signer SHA-256 | `7256fe5a11ade58e3f795516a6ac44d5abbeb3ff3d790068dbf0f87e6023ad75` |

The release description is [v1.0.0.md](v1.0.0.md). `scripts/release-orbit.ps1 -Version 1.0.0 -Publish` uses all of the above automatically.

Creating it by hand instead (after a dry run has produced `dist/`):

```powershell
gh release create "KuralPay/v1.0.0" --repo er-abinesh-21/Release-Orbit --target main `
  --title "KuralPay - v1.0.0" --notes-file docs/release-notes/v1.0.0.md `
  dist/KuralPay-v1.0.0.apk dist/KuralPay-v1.0.0.apk.sha256
```

Keep the tag exactly `KuralPay/vX.Y.Z` and leave the release as a normal (not draft, not pre-release) release; otherwise the app does not see it.
