param(
    [ValidateSet('Build', 'Test')][string]$Task = 'Build',
    [string]$Abi = 'arm64-v8a'
)
$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
# Android's Windows scripts expand !, and ndk-build rejects spaces in paths.
$mapping = & subst.exe | Where-Object { $_ -like 'K:\*' }
if ($mapping) {
    if (($mapping -split ' => ', 2)[1].TrimEnd('\') -ne $projectRoot.TrimEnd('\')) {
        throw 'K: is mapped to another directory; choose a free drive before building.'
    }
} elseif (Test-Path 'K:/') {
    throw 'K: is already in use.'
} else {
    & subst.exe K: $projectRoot
    if ($LASTEXITCODE -ne 0) { throw 'Could not map the project to K:' }
}
$jdk = Get-ChildItem 'K:/.tools/jdk' -Directory | Where-Object { $_.Name -like 'jdk-21*' -and (Test-Path (Join-Path $_.FullName 'bin/javac.exe')) } | Select-Object -First 1
if (!$jdk) { throw 'Install JDK 21 in .tools/jdk first (required for Android 16 tests).' }
$env:JAVA_HOME = $jdk.FullName
$env:ANDROID_HOME = 'K:/.tools/android-sdk'
$env:ANDROID_USER_HOME = 'K:/.tools/android-user-home'
$env:GRADLE_USER_HOME = 'K:/.tools/gradle-user-home'
$wrapper = Get-Content (Join-Path $projectRoot 'gradle/wrapper/gradle-wrapper.properties') -Raw
if ($wrapper -notmatch 'gradle-([\d.]+)-bin') { throw 'Cannot determine pinned Gradle version.' }
$gradle = "K:/.tools/gradle/gradle-$($Matches[1])/bin/gradle.bat"
if (!(Test-Path $gradle)) { throw 'Extract the checksum-verified Gradle distribution into .tools/gradle first.' }
Push-Location 'K:/'
try {
    $options = @('--console=plain', '--stacktrace', '-Dorg.gradle.jvmargs=-Xmx3g -Dfile.encoding=UTF-8', "-Pandroid.injected.build.abi=$Abi")
    if ($Task -eq 'Build') {
        # Development APK must also install from the phone's file manager, without adb -t.
        $options += '-Pandroid.injected.testOnly=false'
        & $gradle :app:assembleDebugNoMinify @options
    } else {
        & $gradle :app:testDebugUnitTest --tests '*LocalDictionaryProviderTest' --tests '*CupertinoDefaultsTest' --tests '*InputPrivacyTest' --tests '*CupertinoBlurBackgroundTest' --tests '*SpellCheckerSessionTest' @options
    }
    if ($LASTEXITCODE -ne 0) { throw "Gradle failed with exit code $LASTEXITCODE" }
} finally {
    Pop-Location
}
