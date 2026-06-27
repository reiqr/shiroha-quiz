<#
.SYNOPSIS
  PDF.js 5.7.284 完整离线包下载脚本
.DESCRIPTION
  从 jsDelivr CDN 下载 PDF.js 到 apps/web/libs/
  包含 CMaps（中日韩字符映射）+ 沙箱文件。
  核心库（pdf.min.mjs / pdf.worker.min.mjs）已内置在仓库中。
  下载后 Web 端 PDF 导入优先使用本地文件，无需联网。
  如果不想下载，CDN 会自动兜底（jsDelivr / unpkg）。
#>

$ErrorActionPreference = "Stop"
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$version = "5.7.284"
$baseUrl = "https://cdn.jsdelivr.net/npm/pdfjs-dist@$version"

# CMaps — 中日韩字符映射（约 166 个文件，~2 MB）
$cmaps = @(
    "78-EUC-H.bcmap","78-EUC-V.bcmap","78-H.bcmap","78-RKSJ-H.bcmap","78-RKSJ-V.bcmap",
    "78-V.bcmap","78ms-RKSJ-H.bcmap","78ms-RKSJ-V.bcmap","83pv-RKSJ-H.bcmap","90ms-RKSJ-H.bcmap",
    "90ms-RKSJ-V.bcmap","90msp-RKSJ-H.bcmap","90msp-RKSJ-V.bcmap","90pv-RKSJ-H.bcmap","90pv-RKSJ-V.bcmap",
    "Add-H.bcmap","Add-RKSJ-H.bcmap","Add-RKSJ-V.bcmap","Add-V.bcmap","Adobe-CNS1-0.bcmap",
    "Adobe-CNS1-1.bcmap","Adobe-CNS1-2.bcmap","Adobe-CNS1-3.bcmap","Adobe-CNS1-4.bcmap",
    "Adobe-CNS1-5.bcmap","Adobe-CNS1-6.bcmap","Adobe-CNS1-UCS2.bcmap","Adobe-GB1-0.bcmap",
    "Adobe-GB1-1.bcmap","Adobe-GB1-2.bcmap","Adobe-GB1-3.bcmap","Adobe-GB1-4.bcmap",
    "Adobe-GB1-5.bcmap","Adobe-GB1-UCS2.bcmap","Adobe-Japan1-0.bcmap","Adobe-Japan1-1.bcmap",
    "Adobe-Japan1-2.bcmap","Adobe-Japan1-3.bcmap","Adobe-Japan1-4.bcmap","Adobe-Japan1-5.bcmap",
    "Adobe-Japan1-6.bcmap","Adobe-Japan1-UCS2.bcmap","Adobe-Korea1-0.bcmap","Adobe-Korea1-1.bcmap",
    "Adobe-Korea1-2.bcmap","Adobe-Korea1-UCS2.bcmap","B5-H.bcmap","B5-V.bcmap","B5pc-H.bcmap",
    "B5pc-V.bcmap","CNS-EUC-H.bcmap","CNS-EUC-V.bcmap","CNS1-H.bcmap","CNS1-V.bcmap","CNS2-H.bcmap",
    "CNS2-V.bcmap","ETHK-B5-H.bcmap","ETHK-B5-V.bcmap","ETen-B5-H.bcmap","ETen-B5-V.bcmap",
    "ETenms-B5-H.bcmap","ETenms-B5-V.bcmap","EUC-H.bcmap","EUC-V.bcmap","Ext-H.bcmap","Ext-RKSJ-H.bcmap",
    "Ext-RKSJ-V.bcmap","Ext-V.bcmap","GB-EUC-H.bcmap","GB-EUC-V.bcmap","GB-H.bcmap","GB-V.bcmap",
    "GBK-EUC-H.bcmap","GBK-EUC-V.bcmap","GBK2K-H.bcmap","GBK2K-V.bcmap","GBKp-EUC-H.bcmap",
    "GBKp-EUC-V.bcmap","GBT-EUC-H.bcmap","GBT-EUC-V.bcmap","GBT-H.bcmap","GBT-V.bcmap",
    "GBTpc-EUC-H.bcmap","GBTpc-EUC-V.bcmap","H.bcmap","HKdla-B5-H.bcmap","HKdla-B5-V.bcmap",
    "HKdlb-B5-H.bcmap","HKdlb-B5-V.bcmap","HKgccs-B5-H.bcmap","HKgccs-B5-V.bcmap",
    "HKm314-B5-H.bcmap","HKm314-B5-V.bcmap","HKm471-B5-H.bcmap","HKm471-B5-V.bcmap",
    "HKscs-B5-H.bcmap","HKscs-B5-V.bcmap","Hankaku.bcmap","Hiragana.bcmap","Hojo-EUC-H.bcmap",
    "Hojo-EUC-V.bcmap","Hojo-H.bcmap","Hojo-V.bcmap","KSC-EUC-H.bcmap","KSC-EUC-V.bcmap",
    "KSC-H.bcmap","KSC-Johab-H.bcmap","KSC-Johab-V.bcmap","KSC-V.bcmap","KSCms-UHC-H.bcmap",
    "KSCms-UHC-HW-H.bcmap","KSCms-UHC-HW-V.bcmap","KSCms-UHC-V.bcmap","KSCpc-EUC-H.bcmap",
    "KSCpc-EUC-UCS2C.bcmap","KSCpc-EUC-V.bcmap","Katakana.bcmap","NWP-H.bcmap","NWP-V.bcmap",
    "RKSJ-H.bcmap","RKSJ-V.bcmap","Roman.bcmap","Shift.bcmap","UniCNS-UCS2-H.bcmap",
    "UniCNS-UCS2-V.bcmap","UniCNS-UTF16-H.bcmap","UniCNS-UTF16-V.bcmap","UniCNS-UTF32-H.bcmap",
    "UniCNS-UTF32-V.bcmap","UniCNS-UTF8-H.bcmap","UniCNS-UTF8-V.bcmap","UniGB-UCS2-H.bcmap",
    "UniGB-UCS2-V.bcmap","UniGB-UTF16-H.bcmap","UniGB-UTF16-V.bcmap","UniGB-UTF32-H.bcmap",
    "UniGB-UTF32-V.bcmap","UniGB-UTF8-H.bcmap","UniGB-UTF8-V.bcmap","UniJIS-UCS2-H.bcmap",
    "UniJIS-UCS2-HW-H.bcmap","UniJIS-UCS2-HW-V.bcmap","UniJIS-UCS2-V.bcmap","UniJIS-UTF16-H.bcmap",
    "UniJIS-UTF16-V.bcmap","UniJIS-UTF32-H.bcmap","UniJIS-UTF32-V.bcmap","UniJIS-UTF8-H.bcmap",
    "UniJIS-UTF8-V.bcmap","UniJIS2004-UTF16-H.bcmap","UniJIS2004-UTF16-V.bcmap",
    "UniJIS2004-UTF32-H.bcmap","UniJIS2004-UTF32-V.bcmap","UniJIS2004-UTF8-H.bcmap",
    "UniJIS2004-UTF8-V.bcmap","UniJISPro-UCS2-HW-V.bcmap","UniJISPro-UCS2-V.bcmap",
    "UniJISPro-UTF8-V.bcmap","UniJISX0213-UTF32-H.bcmap","UniJISX0213-UTF32-V.bcmap",
    "UniJISX02132004-UTF32-H.bcmap","UniJISX02132004-UTF32-V.bcmap","UniKS-UCS2-H.bcmap",
    "UniKS-UCS2-V.bcmap","UniKS-UTF16-H.bcmap","UniKS-UTF16-V.bcmap","UniKS-UTF32-H.bcmap",
    "UniKS-UTF32-V.bcmap","UniKS-UTF8-H.bcmap","UniKS-UTF8-V.bcmap","V.bcmap",
    "WP-Symbol.bcmap","Wansung-EUC-H.bcmap","Wansung-EUC-V.bcmap","Wansung-H.bcmap","Wansung-V.bcmap"
)

Write-Host "PDF.js $version CMaps 下载" -ForegroundColor Cyan
Write-Host "目标位置：$scriptDir/cmaps/" -ForegroundColor Gray

$cmapDir = Join-Path $scriptDir "cmaps"
if (Test-Path $cmapDir) {
    $existing = (Get-ChildItem $cmapDir -File | Measure-Object).Count
    if ($existing -ge $cmaps.Count / 2) {
        Write-Host "已存在 $existing 个 CMaps 文件，跳过下载。" -ForegroundColor Yellow
        Write-Host "如需重新下载，请先删除 cmaps/ 目录再运行此脚本。"
        exit 0
    }
}

if (!(Test-Path $cmapDir)) { New-Item -ItemType Directory -Path $cmapDir -Force | Out-Null }

$total = $cmaps.Count
$done = 0
$downloaded = 0

foreach ($name in $cmaps) {
    $done++
    $url = "$baseUrl/cmaps/$name"
    $outPath = Join-Path $cmapDir $name
    Write-Progress -Activity "下载 PDF.js CMaps" -Status "$done / $total  $name" -PercentComplete ($done * 100 / $total)
    try {
        Invoke-WebRequest -Uri $url -OutFile $outPath -UseBasicParsing -ErrorAction Stop
        $downloaded++
    } catch {
        # 静默跳过不存在的 cmap
    }
}

Write-Progress -Activity "下载 PDF.js CMaps" -Completed
Write-Host ""
Write-Host "完成：$downloaded / $total 个 CMaps 文件已下载。" -ForegroundColor Green
Write-Host "Web 端 PDF 导入将优先使用本地完整 PDF.js（含 CJK 字符映射）。" -ForegroundColor Gray
