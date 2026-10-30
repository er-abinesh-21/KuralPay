<#
  Build, validate and (optionally) publish a KuralPay release to Release Orbit
  (er-abinesh-21/Release-Orbit) as tag KuralPay/vX.Y.Z. Runs on this PC: no
  GitHub secrets, no tokens in the app.

  Signing uses the keystore that signed every installed KuralPay
  (~/.android/debug.keystore unless -Keystore is given). Publishing uses your
  own `gh` login.

  Dry run (default): tests, signed build, validation, dist/ output. Nothing is uploaded.
      .\scripts\release-orbit.ps1 -Version 1.0.1
  Publish (asks for confirmation):
      .\scripts\release-orbit.ps1 -Version 1.0.1 -Publish
#>
param(
    [Parameter(Mandatory = $true)][string]$Version,
    [switch]$Publish,
    [string]$Keystore = (Join-Path $HOME ".android\debug.keystore"),
    [string]$KeyAlias = "androiddebugkey",
    [string]$StorePassword = "android",
    [string]$KeyPassword = "android"
)

# Not "Stop": Windows PowerShell turns any stderr from gh/gradle into a fatal error.
# Every step below checks $LASTEXITCODE or its result explicitly instead.
$ErrorActionPreference = "Continue"
$Orbit = "er-abinesh-21/Release-Orbit"
$TagPrefix = "KuralPay/v"
$AppId = "com.kuralpay.app"
# Signing identity of every KuralPay APK ever installed. Must never change.
$SignerSha256 = "7256fe5a11ade58e3f795516a6ac44d5abbeb3ff3d790068dbf0f87e6023ad75"

$Root = Split-Path $PSScriptRoot -Parent
Set-Location $Root
function Fail($msg) { Write-Host "ERROR: $msg" -ForegroundColor Red; exit 1 }
function Step($msg) { Write-Host "`n== $msg" -ForegroundColor Cyan }

# --- Version ---------------------------------------------------------------
Step "Version"
if ($Version -notmatch '^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)$') { Fail "version must be X.Y.Z (got '$Version')" }
$expectedCode = [int]$Matches[1] * 10000 + [int]$Matches[2] * 100 + [int]$Matches[3]
$gradle = Get-Content app\build.gradle.kts -Raw
$name = [regex]::Match($gradle, '(?m)^\s*versionName\s*=\s*"([^"]+)"').Groups[1].Value
$code = [regex]::Match($gradle, '(?m)^\s*versionCode\s*=\s*(\d+)').Groups[1].Value
if ($name -ne $Version) { Fail "app/build.gradle.kts versionName is '$name', expected '$Version'" }
if ($code -ne "$expectedCode") { Fail "app/build.gradle.kts versionCode is '$code', expected $expectedCode" }
$Tag = "$TagPrefix$Version"
$ApkName = "KuralPay-v$Version.apk"
Write-Host "$Tag  versionCode $code"

# --- Preconditions -----------------------------------------------------------
Step "Checks"
if (-not (Get-Command gh -ErrorAction SilentlyContinue)) { Fail "GitHub CLI (gh) not found" }
gh release view $Tag --repo $Orbit *> $null
if ($LASTEXITCODE -eq 0) { Fail "Release Orbit already has $Tag" }
if (-not (Test-Path app\google-services.json)) { Fail "app/google-services.json is missing (sign-in and sync would be off)" }
if (-not (Test-Path $Keystore)) { Fail "keystore not found: $Keystore" }
if (-not $env:JAVA_HOME) {
    $jbr = Join-Path $HOME ".jdks\jbr-21.0.11"
    if (Test-Path $jbr) { $env:JAVA_HOME = $jbr } else { Fail "set JAVA_HOME to a JDK 21" }
}
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
$sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { Join-Path $env:LOCALAPPDATA "Android\Sdk" }
$bt = Get-ChildItem (Join-Path $sdk "build-tools") -Directory | Sort-Object { [version]$_.Name } | Select-Object -Last 1
if (-not $bt) { Fail "Android build-tools not found under $sdk" }
$aapt2 = Join-Path $bt.FullName "aapt2.exe"
$apksigner = Join-Path $bt.FullName "apksigner.bat"
Write-Host "ok (JDK $env:JAVA_HOME, build-tools $($bt.Name))"

# --- Build -----------------------------------------------------------------
Step "Unit tests and signed release build"
$env:KURALPAY_KEYSTORE_FILE = $Keystore
$env:KURALPAY_KEYSTORE_PASSWORD = $StorePassword
$env:KURALPAY_KEY_ALIAS = $KeyAlias
$env:KURALPAY_KEY_PASSWORD = $KeyPassword
try {
    & .\gradlew.bat --no-daemon testDebugUnitTest assembleRelease
    if ($LASTEXITCODE -ne 0) { Fail "Gradle build failed" }
} finally {
    Remove-Item Env:KURALPAY_KEYSTORE_FILE, Env:KURALPAY_KEYSTORE_PASSWORD, Env:KURALPAY_KEY_ALIAS, Env:KURALPAY_KEY_PASSWORD -ErrorAction SilentlyContinue
}

# --- Validate --------------------------------------------------------------
Step "Validate APK"
$src = "app\build\outputs\apk\release\app-release.apk"
if (-not (Test-Path $src)) { Fail "$src not found (was it signed?)" }
New-Item -ItemType Directory -Force dist | Out-Null
$apk = Join-Path $Root "dist\$ApkName"
Copy-Item $src $apk -Force

function Badging($path) {
    $line = (& $aapt2 dump badging $path | Select-Object -First 1)
    $all = (& $aapt2 dump badging $path) -join "`n"
    [pscustomobject]@{
        Package     = [regex]::Match($line, "name='([^']+)'").Groups[1].Value
        VersionCode = [int][regex]::Match($line, "versionCode='(\d+)'").Groups[1].Value
        VersionName = [regex]::Match($line, "versionName='([^']+)'").Groups[1].Value
        Debuggable  = $all -match "application-debuggable"
    }
}
$b = Badging $apk
if ($b.Package -ne $AppId) { Fail "package is $($b.Package)" }
if ($b.VersionName -ne $Version) { Fail "versionName is $($b.VersionName)" }
if ($b.VersionCode -ne $expectedCode) { Fail "versionCode is $($b.VersionCode)" }
if ($b.Debuggable) { Fail "APK is debuggable" }

$certs = (cmd /c "`"$apksigner`" verify --print-certs `"$apk`"") -join "`n"
if ($LASTEXITCODE -ne 0) { Fail "apksigner verification failed" }
$signer = [regex]::Match($certs, 'Signer #1 certificate SHA-256 digest: ([0-9a-f]+)').Groups[1].Value
if ($signer -ne $SignerSha256) { Fail "signed with '$signer', expected $SignerSha256" }
Write-Host "package $($b.Package), $($b.VersionName) ($($b.VersionCode)), not debuggable, signer matches"

# versionCode must exceed the highest published KuralPay release (exact tag prefix only).
$tags = gh api "repos/$Orbit/releases?per_page=100" --paginate --jq ".[] | select(.draft|not) | select(.prerelease|not) | .tag_name"
if ($LASTEXITCODE -ne 0) { Fail "could not list Release Orbit releases" }
$prev = $tags | Where-Object { $_ -match '^KuralPay/v(\d+)\.(\d+)\.(\d+)$' } |
    Sort-Object { $v = $_.Substring($TagPrefix.Length).Split('.'); [int]$v[0] * 10000 + [int]$v[1] * 100 + [int]$v[2] } |
    Select-Object -Last 1
if ($prev) {
    $tmp = Join-Path ([IO.Path]::GetTempPath()) ("kuralpay-prev-" + [guid]::NewGuid())
    gh release download $prev --repo $Orbit --pattern "KuralPay-v*.apk" --dir $tmp
    if ($LASTEXITCODE -ne 0) { Fail "could not download $prev" }
    $prevCode = (Badging (Get-ChildItem $tmp -Filter *.apk | Select-Object -First 1).FullName).VersionCode
    Remove-Item $tmp -Recurse -Force
    Write-Host "previous release $prev has versionCode $prevCode"
    if ($b.VersionCode -le $prevCode) { Fail "versionCode $($b.VersionCode) is not greater than $prevCode" }
} else {
    Write-Host "no earlier KuralPay release on Release Orbit"
}

$hash = (Get-FileHash $apk -Algorithm SHA256).Hash.ToLower()
$shaFile = "$apk.sha256"
[IO.File]::WriteAllText($shaFile, "$hash  $ApkName`n", (New-Object Text.UTF8Encoding($false)))
Write-Host "sha256 $hash"
Write-Host ("size   {0:N1} MB" -f ((Get-Item $apk).Length / 1MB))

if (-not $Publish) {
    Step "Dry run complete"
    Write-Host "Built dist\$ApkName. Nothing was uploaded. Re-run with -Publish to release $Tag."
    exit 0
}

# --- Publish ---------------------------------------------------------------
Step "Publish $Tag to $Orbit"
$notes = "docs\release-notes\v$Version.md"
if (-not (Test-Path $notes)) {
    $notes = Join-Path ([IO.Path]::GetTempPath()) "kuralpay-notes-$Version.md"
    [IO.File]::WriteAllText($notes, "# KuralPay v$Version`n`nImprovements and fixes.`n", (New-Object Text.UTF8Encoding($false)))
}
$answer = Read-Host "Publish $Tag publicly to $Orbit? Type the version ($Version) to confirm"
if ($answer -ne $Version) { Fail "not confirmed; nothing was published" }

gh release create $Tag --repo $Orbit --target main --title "KuralPay - v$Version" --notes-file $notes $apk $shaFile
if ($LASTEXITCODE -ne 0) { Fail "gh release create failed" }

Step "Verify public download"
$url = "https://github.com/$Orbit/releases/download/$Tag/$ApkName"
$check = Join-Path ([IO.Path]::GetTempPath()) "kuralpay-public-$Version.apk"
$ok = $false
for ($i = 0; $i -lt 5 -and -not $ok; $i++) {
    try { Invoke-WebRequest $url -OutFile $check -UseBasicParsing; $ok = $true } catch { Start-Sleep 5 }
}
if (-not $ok) { Fail "public download failed: $url" }
$publicHash = (Get-FileHash $check -Algorithm SHA256).Hash.ToLower()
Remove-Item $check -Force
if ($publicHash -ne $hash) { Fail "public APK hash mismatch" }
$rel = Invoke-RestMethod "https://api.github.com/repos/$Orbit/releases/tags/$Tag" -UseBasicParsing
$matching = @($rel.assets | Where-Object { $_.name -eq $ApkName }).Count
if ($rel.draft -or $rel.prerelease -or $matching -ne 1) { Fail "release metadata check failed" }
Write-Host "Published: $url" -ForegroundColor Green
