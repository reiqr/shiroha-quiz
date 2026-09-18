@echo off
chcp 65001 >nul
cd /d "%~dp0"

rem =====================================================================
rem  Shiroha Quiz - Web edition launcher (Windows)
rem
rem  Why not just double-click index.html:
rem    OCR and PDF parsing rely on Web Worker and ES modules, which
rem    browsers refuse to load over file://. Opening index.html directly
rem    still allows practice, but those two features break, and the data
rem    is saved under a different origin than the one used here.
rem
rem  Startup order:
rem    1. Python, if present (py -3 first, then python)
rem    2. Otherwise Windows built-in PowerShell, via serve.ps1
rem
rem  Both paths listen on exactly http://127.0.0.1:51735
rem    Do not change the port: browsers isolate storage by
rem    scheme + host + port, so another port means another origin and
rem    the existing question bank would no longer be visible.
rem
rem  Keep this file ASCII-only on purpose.
rem    cmd.exe reads batch files by byte offset. Non-ASCII bytes under
rem    chcp 65001 make it lose sync and execute fragments of the comments
rem    as commands. All Chinese text lives in serve.ps1 instead.
rem =====================================================================

set "PORT=51735"
set "URL=http://127.0.0.1:%PORT%/index.html"
set "PYTHON_CMD="

where py >nul 2>nul
if not errorlevel 1 set "PYTHON_CMD=py -3"

if not defined PYTHON_CMD (
  where python >nul 2>nul
  if not errorlevel 1 set "PYTHON_CMD=python"
)

rem Skip startup when a server already answers on this port
powershell -NoProfile -ExecutionPolicy Bypass -Command "try { $r = Invoke-WebRequest -UseBasicParsing -TimeoutSec 1 '%URL%'; if ($r.StatusCode -ge 200) { exit 0 } else { exit 1 } } catch { exit 1 }" >nul 2>nul

if errorlevel 1 (
  if defined PYTHON_CMD (
    start "Shiroha Quiz Local Server" /min cmd /k "%PYTHON_CMD% -m http.server %PORT% --bind 127.0.0.1"
  ) else (
    start "Shiroha Quiz Local Server" /min powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0serve.ps1" -Port %PORT%
  )
  rem ping, not timeout: timeout fails when stdin is redirected
  ping -n 3 127.0.0.1 >nul
)

start "" "%URL%"
