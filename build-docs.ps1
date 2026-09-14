<#
.SYNOPSIS
    把 docs 下的用户文档从 Markdown 转换为 Word (.docx)。

.DESCRIPTION
    使用 pandoc 转换。无参数时转换全部面向用户的文档目录；
    开发者文档（native / universal / CODEMAPS / archive 等）不在默认范围内。

    依赖：pandoc
    安装：winget install --id JohnMacFarlane.Pandoc

.PARAMETER Path
    要转换的 Markdown 文件或目录，可指定多个。
    不指定时使用默认的用户文档目录列表。

.PARAMETER ReferenceDoc
    参考样式文档。指定后生成的 docx 会套用该文档中的样式定义。
    可用 Word 打开任意一份满意的 docx，调好样式后另存为模板使用。

.EXAMPLE
    .\build-docs.ps1
    转换全部用户文档。

.EXAMPLE
    .\build-docs.ps1 -Path 'docs/WebDAV云备份使用指南'
    只转换指定目录。

.EXAMPLE
    .\build-docs.ps1 -ReferenceDoc 'docs/_reference.docx'
    套用自定义样式模板转换。
#>
[CmdletBinding()]
param(
    [string[]]$Path,
    [string]$ReferenceDoc
)

$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot

# 默认转换的用户文档目录（相对项目根）。
# 新增面向用户的文档目录时，在这里补一行即可。
$DefaultTargets = @(
    'docs/标准题库格式示例',
    'docs/题库导入格式支持说明',
    'docs/题库导入策略与使用指南',
    'docs/题目导入解析方法说明',
    'docs/WebDAV云备份使用指南'
)

function Get-PandocPath {
    $cmd = Get-Command pandoc -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    $fallback = Join-Path $env:LOCALAPPDATA 'Pandoc\pandoc.exe'
    if (Test-Path -LiteralPath $fallback) { return $fallback }
    throw '未找到 pandoc。请先安装：winget install --id JohnMacFarlane.Pandoc'
}

$pandocExe = Get-PandocPath
Write-Host "pandoc：$pandocExe" -ForegroundColor DarkGray

$targets = if ($Path) { $Path } else { $DefaultTargets }
$files = [System.Collections.Generic.List[System.IO.FileInfo]]::new()

foreach ($target in $targets) {
    $resolved = if ([System.IO.Path]::IsPathRooted($target)) { $target } else { Join-Path $root $target }
    if (-not (Test-Path -LiteralPath $resolved)) {
        Write-Warning "跳过不存在的路径：$target"
        continue
    }
    $item = Get-Item -LiteralPath $resolved
    if ($item.PSIsContainer) {
        Get-ChildItem -LiteralPath $item.FullName -Filter '*.md' -File |
            ForEach-Object { $files.Add($_) }
    }
    elseif ($item.Extension -eq '.md') {
        $files.Add($item)
    }
    else {
        Write-Warning "跳过非 Markdown 文件：$target"
    }
}

if ($files.Count -eq 0) {
    Write-Warning '没有找到要转换的 Markdown 文件。'
    exit 0
}

$pandocArgs = @('--from=gfm')
if ($ReferenceDoc) {
    $refResolved = if ([System.IO.Path]::IsPathRooted($ReferenceDoc)) { $ReferenceDoc } else { Join-Path $root $ReferenceDoc }
    if (-not (Test-Path -LiteralPath $refResolved)) { throw "参考样式文件不存在：$ReferenceDoc" }
    $pandocArgs += "--reference-doc=$refResolved"
    Write-Host "参考样式：$ReferenceDoc" -ForegroundColor DarkGray
}

$succeeded = 0
$failed = 0

foreach ($file in $files) {
    $output = [System.IO.Path]::ChangeExtension($file.FullName, '.docx')
    Write-Host "转换：$($file.Name) " -NoNewline
    & $pandocExe $file.FullName '-o' $output @pandocArgs
    if ($LASTEXITCODE -eq 0 -and (Test-Path -LiteralPath $output)) {
        $sizeKb = [Math]::Round((Get-Item -LiteralPath $output).Length / 1KB, 1)
        Write-Host "→ 完成（$sizeKb KB）" -ForegroundColor Green
        $succeeded++
    }
    else {
        Write-Host "→ 失败（exit $LASTEXITCODE）" -ForegroundColor Red
        $failed++
    }
}

Write-Host ''
Write-Host "完成：$succeeded 个成功，$failed 个失败。"
if ($failed -gt 0) { exit 1 }
