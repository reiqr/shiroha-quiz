<#
.SYNOPSIS
  PDF.js 5.7.284 离线包下载脚本
.DESCRIPTION
  从 jsDelivr CDN 下载 PDF.js 到 apps/web/libs/
  下载后 Web 端 PDF 导入优先使用本地文件，无需联网。
  如果不想下载，CDN 会自动兜底（jsDelivr / unpkg）。
#>

$ErrorActionPreference = "Stop"
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$version = "5.7.284"
$files = @{
    "pdf.min.mjs"         = "https://cdn.jsdelivr.net/npm/pdfjs-dist@$version/build/pdf.min.mjs"
    "pdf.worker.min.mjs"  = "https://cdn.jsdelivr.net/npm/pdfjs-dist@$version/build/pdf.worker.min.mjs"
}

Write-Host "PDF.js $version 离线包下载" -ForegroundColor Cyan
Write-Host "目标位置：$scriptDir" -ForegroundColor Gray

$allExist = $true
foreach ($name in $files.Keys) {
    if (!(Test-Path (Join-Path $scriptDir $name))) { $allExist = $false; break }
}

if ($allExist) {
    Write-Host "已存在所有文件，跳过下载。" -ForegroundColor Yellow
    Write-Host "如需重新下载，请先删除 pdf.min.mjs 和 pdf.worker.min.mjs 再运行此脚本。"
    exit 0
}

foreach ($entry in $files.GetEnumerator()) {
    $name = $entry.Key
    $url = $entry.Value
    $outPath = Join-Path $scriptDir $name
    Write-Host "下载 $name ..." -NoNewline
    try {
        Invoke-WebRequest -Uri $url -OutFile $outPath -UseBasicParsing -ErrorAction Stop
        Write-Host " 完成" -ForegroundColor Green
    } catch {
        Write-Host " 失败" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "完成。Web 端 PDF 导入将优先使用本地 PDF.js。" -ForegroundColor Green
