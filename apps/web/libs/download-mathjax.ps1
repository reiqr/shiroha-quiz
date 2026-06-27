<#
.SYNOPSIS
  MathJax 3.2.2 离线包下载脚本
.DESCRIPTION
  从 jsDelivr CDN 下载 MathJax 3.2.2 es5 组件到 apps/web/libs/mathjax/
  下载后 Web 端公式优先使用本地文件渲染，无需联网。
  如果不想下载，CDN 会自动兜底。
#>

$ErrorActionPreference = "Stop"
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$targetDir = Join-Path $scriptDir "mathjax"

Write-Host "MathJax 3.2.2 离线包下载" -ForegroundColor Cyan
Write-Host "目标位置：$targetDir" -ForegroundColor Gray

if (Test-Path $targetDir) {
    $existing = (Get-ChildItem $targetDir -Recurse -File | Measure-Object).Count
    Write-Host "已存在 $existing 个文件，跳过下载。" -ForegroundColor Yellow
    Write-Host "如需重新下载，请先删除 mathjax/ 目录再运行此脚本。"
    exit 0
}

$baseUrl = "https://cdn.jsdelivr.net/npm/mathjax@3.2.2/es5"
$files = @(
    "tex-mml-chtml.js",
    "tex-chtml.js",
    "tex-chtml-full.js",
    "tex-chtml-full-speech.js",
    "tex-svg.js",
    "tex-svg-full.js",
    "tex-mml-svg.js",
    "mml-chtml.js",
    "mml-svg.js",
    "core.js",
    "loader.js",
    "startup.js",
    "latest.js",
    "node-main.js",
    "ui/lazy.js",
    "ui/menu.js",
    "ui/safe.js",
    "adaptors/liteDOM.js",
    "a11y/assistive-mml.js",
    "a11y/complexity.js",
    "a11y/explorer.js",
    "a11y/semantic-enrich.js",
    "a11y/sre.js",
    "input/asciimath.js",
    "input/mml.js",
    "input/mml/entities.js",
    "input/mml/extensions/mml3.js",
    "input/mml/extensions/mml3.sef.json",
    "input/tex.js",
    "input/tex-base.js",
    "input/tex-full.js",
    "input/tex/extensions/action.js",
    "input/tex/extensions/all-packages.js",
    "input/tex/extensions/ams.js",
    "input/tex/extensions/amscd.js",
    "input/tex/extensions/autoload.js",
    "input/tex/extensions/bbox.js",
    "input/tex/extensions/boldsymbol.js",
    "input/tex/extensions/braket.js",
    "input/tex/extensions/bussproofs.js",
    "input/tex/extensions/cancel.js",
    "input/tex/extensions/cases.js",
    "input/tex/extensions/centernot.js",
    "input/tex/extensions/color.js",
    "input/tex/extensions/colortbl.js",
    "input/tex/extensions/colorv2.js",
    "input/tex/extensions/configmacros.js",
    "input/tex/extensions/empheq.js",
    "input/tex/extensions/enclose.js",
    "input/tex/extensions/extpfeil.js",
    "input/tex/extensions/gensymb.js",
    "input/tex/extensions/html.js",
    "input/tex/extensions/mathtools.js",
    "input/tex/extensions/mhchem.js",
    "input/tex/extensions/newcommand.js",
    "input/tex/extensions/noerrors.js",
    "input/tex/extensions/noundefined.js",
    "input/tex/extensions/physics.js",
    "input/tex/extensions/require.js",
    "input/tex/extensions/setoptions.js",
    "input/tex/extensions/tagformat.js",
    "input/tex/extensions/textcomp.js",
    "input/tex/extensions/textmacros.js",
    "input/tex/extensions/unicode.js",
    "input/tex/extensions/upgreek.js",
    "input/tex/extensions/verb.js",
    "output/chtml.js",
    "output/chtml/fonts/tex.js",
    "output/chtml/fonts/woff-v2/MathJax_AMS-Regular.woff",
    "output/chtml/fonts/woff-v2/MathJax_Calligraphic-Bold.woff",
    "output/chtml/fonts/woff-v2/MathJax_Calligraphic-Regular.woff",
    "output/chtml/fonts/woff-v2/MathJax_Fraktur-Bold.woff",
    "output/chtml/fonts/woff-v2/MathJax_Fraktur-Regular.woff",
    "output/chtml/fonts/woff-v2/MathJax_Main-Bold.woff",
    "output/chtml/fonts/woff-v2/MathJax_Main-Italic.woff",
    "output/chtml/fonts/woff-v2/MathJax_Main-Regular.woff",
    "output/chtml/fonts/woff-v2/MathJax_Math-BoldItalic.woff",
    "output/chtml/fonts/woff-v2/MathJax_Math-Italic.woff",
    "output/chtml/fonts/woff-v2/MathJax_Math-Regular.woff",
    "output/chtml/fonts/woff-v2/MathJax_SansSerif-Bold.woff",
    "output/chtml/fonts/woff-v2/MathJax_SansSerif-Italic.woff",
    "output/chtml/fonts/woff-v2/MathJax_SansSerif-Regular.woff",
    "output/chtml/fonts/woff-v2/MathJax_Script-Regular.woff",
    "output/chtml/fonts/woff-v2/MathJax_Size1-Regular.woff",
    "output/chtml/fonts/woff-v2/MathJax_Size2-Regular.woff",
    "output/chtml/fonts/woff-v2/MathJax_Size3-Regular.woff",
    "output/chtml/fonts/woff-v2/MathJax_Size4-Regular.woff",
    "output/chtml/fonts/woff-v2/MathJax_Typewriter-Regular.woff",
    "output/chtml/fonts/woff-v2/MathJax_Vector-Bold.woff",
    "output/chtml/fonts/woff-v2/MathJax_Vector-Regular.woff",
    "output/chtml/fonts/woff-v2/MathJax_Zero.woff",
    "output/svg.js",
    "output/svg/fonts/tex.js",
    "sre/mathmaps/base.json",
    "sre/mathmaps/ca.json",
    "sre/mathmaps/da.json",
    "sre/mathmaps/de.json",
    "sre/mathmaps/en.json",
    "sre/mathmaps/es.json",
    "sre/mathmaps/fr.json",
    "sre/mathmaps/hi.json",
    "sre/mathmaps/it.json",
    "sre/mathmaps/nb.json",
    "sre/mathmaps/nemeth.json",
    "sre/mathmaps/nn.json",
    "sre/mathmaps/sv.json"
)

$total = $files.Count
$done = 0
$downloaded = 0

foreach ($file in $files) {
    $done++
    $url = "$baseUrl/$file"
    $outPath = Join-Path $targetDir $file
    $outDir = Split-Path -Parent $outPath
    if (!(Test-Path $outDir)) { New-Item -ItemType Directory -Path $outDir -Force | Out-Null }

    Write-Progress -Activity "下载 MathJax 3.2.2" -Status "$done / $total  $file" -PercentComplete ($done * 100 / $total)
    try {
        Invoke-WebRequest -Uri $url -OutFile $outPath -UseBasicParsing -ErrorAction Stop
        $downloaded++
    } catch {
        Write-Host "  失败，跳过：$file" -ForegroundColor Red
    }
}

Write-Progress -Activity "下载 MathJax 3.2.2" -Completed
Write-Host ""
Write-Host "完成：$downloaded / $total 个文件已下载到 $targetDir" -ForegroundColor Green
Write-Host "Web 端将优先使用本地 MathJax，无需联网渲染公式。" -ForegroundColor Gray
