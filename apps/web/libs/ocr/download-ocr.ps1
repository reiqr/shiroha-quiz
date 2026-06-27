<#
.SYNOPSIS
  Shiroha Quiz Web OCR 离线扩展下载脚本
.DESCRIPTION
  下载 Tesseract.js 5.1.1、tesseract.js-core 5.1.1 和中英文语言包。
  下载完成后，Web 版 OCR 会优先使用本地资源；本地资源缺失时仍可使用 CDN 兜底。
#>

$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$version = "5.1.1"
$tempDir = Join-Path $scriptDir ".ocr-download"
$tesseractDir = Join-Path $scriptDir "tesseract"
$coreDir = Join-Path $scriptDir "core"
$langDir = Join-Path $scriptDir "lang"

function Download-File {
    param(
        [Parameter(Mandatory = $true)][string]$Url,
        [Parameter(Mandatory = $true)][string]$OutFile
    )
    Write-Host "  $Url" -ForegroundColor Gray
    Invoke-WebRequest -Uri $Url -OutFile $OutFile -UseBasicParsing -ErrorAction Stop
}

function Reset-Dir {
    param([Parameter(Mandatory = $true)][string]$Path)
    if (Test-Path $Path) {
        Remove-Item -Path $Path -Recurse -Force
    }
    New-Item -ItemType Directory -Path $Path -Force | Out-Null
}

Write-Host "Tesseract.js OCR 离线扩展下载" -ForegroundColor Cyan
Write-Host "目标位置：$scriptDir" -ForegroundColor Gray

Reset-Dir $tempDir
New-Item -ItemType Directory -Path $tesseractDir,$coreDir,$langDir -Force | Out-Null

$tesseractTgz = Join-Path $tempDir "tesseract.js-$version.tgz"
$coreTgz = Join-Path $tempDir "tesseract.js-core-$version.tgz"

Write-Host "`n[下载 Tesseract.js 主包]" -ForegroundColor Yellow
Download-File "https://registry.npmjs.org/tesseract.js/-/tesseract.js-$version.tgz" $tesseractTgz

Write-Host "[下载 Tesseract.js Core]" -ForegroundColor Yellow
Download-File "https://registry.npmjs.org/tesseract.js-core/-/tesseract.js-core-$version.tgz" $coreTgz

$tesseractExtract = Join-Path $tempDir "tesseract-js"
$coreExtract = Join-Path $tempDir "tesseract-core"
New-Item -ItemType Directory -Path $tesseractExtract,$coreExtract -Force | Out-Null

Write-Host "[解包]" -ForegroundColor Yellow
tar -xf $tesseractTgz -C $tesseractExtract
tar -xf $coreTgz -C $coreExtract

Write-Host "[复制运行文件]" -ForegroundColor Yellow
Copy-Item -Force -Path (Join-Path $tesseractExtract "package\dist\tesseract.min.js") -Destination $tesseractDir
Copy-Item -Force -Path (Join-Path $tesseractExtract "package\dist\worker.min.js") -Destination $tesseractDir
Copy-Item -Force -Path (Join-Path $coreExtract "package\*") -Destination $coreDir -Recurse

Write-Host "[下载语言包]" -ForegroundColor Yellow
Download-File "https://tessdata.projectnaptha.com/4.0.0/chi_sim.traineddata.gz" (Join-Path $langDir "chi_sim.traineddata.gz")
Download-File "https://tessdata.projectnaptha.com/4.0.0/eng.traineddata.gz" (Join-Path $langDir "eng.traineddata.gz")

Remove-Item -Path $tempDir -Recurse -Force

Write-Host ""
Write-Host "完成：OCR 离线扩展已更新。" -ForegroundColor Green
Write-Host "Web 版 OCR 会优先使用本地资源；OCR 结果仍需人工核对。" -ForegroundColor Gray

