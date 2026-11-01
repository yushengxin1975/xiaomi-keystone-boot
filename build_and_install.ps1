$ErrorActionPreference = "Stop"

$ProjectDir = $PSScriptRoot
$BuildDir = Join-Path $ProjectDir "build"
$GenDir = Join-Path $BuildDir "gen"
$ObjDir = Join-Path $BuildDir "obj"
$DexDir = Join-Path $BuildDir "dex"

$JavaHome = if ($env:JAVA_HOME) { $env:JAVA_HOME } else { throw "Please set JAVA_HOME environment variable" }
$AndroidSdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } elseif ($env:ANDROID_SDK_ROOT) { $env:ANDROID_SDK_ROOT } else { throw "Please set ANDROID_HOME environment variable" }

$Javac = Join-Path $JavaHome "bin\javac.exe"
$BuildTools = Join-Path $AndroidSdk "build-tools\34.0.0"
$Aapt = Join-Path $BuildTools "aapt.exe"
$D8 = Join-Path $BuildTools "d8.bat"
$Zipalign = Join-Path $BuildTools "zipalign.exe"
$Apksigner = Join-Path $BuildTools "apksigner.bat"
$AndroidJar = Join-Path $AndroidSdk "platforms\android-34\android.jar"

if (Test-Path $BuildDir) {
    Remove-Item -Recurse -Force $BuildDir
}
New-Item -ItemType Directory -Force -Path $GenDir, $ObjDir, $DexDir | Out-Null

Write-Host "[1/5] Generating R.java and packaging resources..."
& $Aapt package -f -m `
    -J $GenDir `
    -M (Join-Path $ProjectDir "AndroidManifest.xml") `
    -S (Join-Path $ProjectDir "res") `
    -I $AndroidJar `
    -F (Join-Path $BuildDir "unaligned.apk")

Write-Host "[2/5] Compiling Java sources..."
$JavaFiles = Get-ChildItem -Path (Join-Path $ProjectDir "src"), $GenDir -Recurse -Filter "*.java" | ForEach-Object { $_.FullName }
& $Javac -encoding UTF-8 -source 1.8 -target 1.8 -bootclasspath $AndroidJar -d $ObjDir $JavaFiles

Write-Host "[3/5] Converting .class files to classes.dex..."
$ClassFiles = Get-ChildItem -Path $ObjDir -Recurse -Filter "*.class" | ForEach-Object { $_.FullName }
& $D8 --lib $AndroidJar --min-api 26 --output $DexDir $ClassFiles

Write-Host "[4/5] Adding classes.dex to APK and aligning..."
Push-Location $DexDir
& $Aapt add (Join-Path $BuildDir "unaligned.apk") "classes.dex"
Pop-Location

$AlignedApk = Join-Path $BuildDir "aligned.apk"
& $Zipalign -f 4 (Join-Path $BuildDir "unaligned.apk") $AlignedApk

Write-Host "[5/5] Signing APK (requires KEYSTORE_FILE, KEYSTORE_ALIAS, and KEYSTORE_PASS env vars if signing)..."
$FinalApk = Join-Path $ProjectDir "XiaomiKeystoneBoot.apk"
if ($env:KEYSTORE_FILE -and $env:KEYSTORE_PASS -and $env:KEYSTORE_ALIAS) {
    & $Apksigner sign `
        --ks $env:KEYSTORE_FILE `
        --ks-pass "env:KEYSTORE_PASS" `
        --key-pass "env:KEYSTORE_PASS" `
        --ks-key-alias $env:KEYSTORE_ALIAS `
        --out $FinalApk `
        $AlignedApk
    Write-Host "Built and signed successfully: $FinalApk"
} else {
    Copy-Item $AlignedApk $FinalApk -Force
    Write-Host "Built unsigned APK: $FinalApk (set KEYSTORE_FILE, KEYSTORE_ALIAS, KEYSTORE_PASS to sign)"
}
