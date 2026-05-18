$ErrorActionPreference = 'Stop'

$ScriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$RegressionDir = Join-Path $ScriptRoot 'test\native-parser-regression'
$ActualDir = Join-Path $RegressionDir 'actual'

function Resolve-GradleCommand {
    if ($env:SHIROHA_GRADLE -and (Test-Path $env:SHIROHA_GRADLE)) {
        return (Resolve-Path $env:SHIROHA_GRADLE).Path
    }

    $ProjectGradlew = Join-Path $ScriptRoot 'apps\android\gradlew.bat'
    if (Test-Path $ProjectGradlew) {
        return (Resolve-Path $ProjectGradlew).Path
    }

    throw '未找到 Gradle。请确认 apps/android/gradlew.bat 存在，或设置环境变量 SHIROHA_GRADLE。'
}

if (Test-Path $ActualDir) {
    Remove-Item -LiteralPath (Join-Path $ActualDir '*.json') -Force -ErrorAction SilentlyContinue
    Remove-Item -LiteralPath (Join-Path $ActualDir 'REGRESSION_REPORT.md') -Force -ErrorAction SilentlyContinue
}
else {
    New-Item -ItemType Directory -Path $ActualDir | Out-Null
}

$Gradle = Resolve-GradleCommand
$env:GRADLE_USER_HOME = Join-Path $ScriptRoot 'apps\android\.gradle-user-home'

Write-Host "Using Gradle: $Gradle"
Write-Host "Cleaning generated actual outputs: $ActualDir"

Push-Location (Join-Path $RegressionDir 'runner')
try {
    & $Gradle --offline run
}
finally {
    Pop-Location
}

python (Join-Path $RegressionDir 'tools\compare_regression.py')
