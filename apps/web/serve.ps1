<#
.SYNOPSIS
    Shiroha Quiz Web 版 —— 无 Python 环境下的本地静态服务。

.DESCRIPTION
    由「打开Shiroha Quiz.cmd」在系统没有 Python 时调用，
    用 Windows 自带的 PowerShell + HttpListener 提供静态文件服务，
    让「解压即用」不再要求用户先装 Python。

    监听地址与 Python 方案完全一致：http://127.0.0.1:51735
    这一点不能改 —— 浏览器按「协议 + 主机 + 端口」隔离 LocalStorage，
    换端口等于换来源，用户已有的题库会全部看不见。
    只绑定回环地址，不对局域网暴露。

    与 Python 的 http.server 相比，本脚本不支持 Range 断点续传，
    也不输出访问日志；对本应用的静态资源加载没有影响。

.PARAMETER Port
    监听端口，默认 51735。除非端口确实被占用，否则不要修改。

.EXAMPLE
    powershell -NoProfile -ExecutionPolicy Bypass -File .\启动服务.ps1
#>
[CmdletBinding()]
param([int]$Port = 51735)

$ErrorActionPreference = 'Stop'
$rootPrefix = [System.IO.Path]::GetFullPath($PSScriptRoot).TrimEnd('\') + '\'

# 扩展名 → MIME 类型。
# .mjs 和 .wasm 必须正确：前者是 ES module（PDF.js 主模块），
# 后者供 PDF.js 走 WebAssembly.instantiateStreaming。
# 这两处 MIME 不对，浏览器会直接拒绝加载，而不是降级。
$MimeMap = @{
    '.html'  = 'text/html; charset=utf-8'
    '.js'    = 'text/javascript; charset=utf-8'
    '.mjs'   = 'text/javascript; charset=utf-8'
    '.css'   = 'text/css; charset=utf-8'
    '.json'  = 'application/json; charset=utf-8'
    '.txt'   = 'text/plain; charset=utf-8'
    '.svg'   = 'image/svg+xml'
    '.png'   = 'image/png'
    '.jpg'   = 'image/jpeg'
    '.jpeg'  = 'image/jpeg'
    '.gif'   = 'image/gif'
    '.webp'  = 'image/webp'
    '.ico'   = 'image/x-icon'
    '.wasm'  = 'application/wasm'
    '.woff'  = 'font/woff'
    '.woff2' = 'font/woff2'
    '.ttf'   = 'font/ttf'
    '.otf'   = 'font/otf'
    '.pdf'   = 'application/pdf'
    '.docx'  = 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
    '.xlsx'  = 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    '.zip'   = 'application/zip'
    '.bcmap' = 'application/octet-stream'
    '.pfb'   = 'application/octet-stream'
}

$listener = New-Object System.Net.HttpListener
$listener.Prefixes.Add("http://127.0.0.1:$Port/")
$listener.Start()
Write-Host "Shiroha Quiz 本地服务已启动：http://127.0.0.1:$Port/"
Write-Host '关闭本窗口即停止服务。'

try {
    while ($listener.IsListening) {
        $context = $listener.GetContext()
        $response = $context.Response
        try {
            $relative = [System.Uri]::UnescapeDataString($context.Request.Url.AbsolutePath).TrimStart('/')
            if ([string]::IsNullOrEmpty($relative)) { $relative = 'index.html' }

            $full = [System.IO.Path]::GetFullPath((Join-Path $rootPrefix $relative))

            if (-not $full.StartsWith($rootPrefix, [System.StringComparison]::OrdinalIgnoreCase)) {
                # 目录穿越：规范化后跑到了应用目录之外
                $response.StatusCode = 403
            }
            elseif (Test-Path -LiteralPath $full -PathType Leaf) {
                $extension = [System.IO.Path]::GetExtension($full).ToLowerInvariant()
                $contentType = 'application/octet-stream'
                if ($MimeMap.ContainsKey($extension)) { $contentType = $MimeMap[$extension] }
                $response.ContentType = $contentType
                $bytes = [System.IO.File]::ReadAllBytes($full)
                $response.ContentLength64 = $bytes.Length
                $response.OutputStream.Write($bytes, 0, $bytes.Length)
            }
            else {
                $response.StatusCode = 404
            }
        }
        catch {
            try { $response.StatusCode = 500 } catch { }
        }
        finally {
            $response.Close()
        }
    }
}
finally {
    $listener.Stop()
    $listener.Close()
}
