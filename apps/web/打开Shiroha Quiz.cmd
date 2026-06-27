@echo off
chcp 65001 >nul
cd /d "%~dp0"

set "PORT=51735"
set "URL=http://127.0.0.1:%PORT%/index.html"
set "PYTHON_CMD="

where py >nul 2>nul
if not errorlevel 1 set "PYTHON_CMD=py -3"

if not defined PYTHON_CMD (
  where python >nul 2>nul
  if not errorlevel 1 set "PYTHON_CMD=python"
)

if not defined PYTHON_CMD (
  echo 未找到 Python，无法启动本地网页服务。
  echo 请安装 Python 后重试，或手动使用其他本地 HTTP 服务打开本目录。
  pause
  exit /b 1
)

powershell -NoProfile -ExecutionPolicy Bypass -Command "try { $r = Invoke-WebRequest -UseBasicParsing -TimeoutSec 1 '%URL%'; if ($r.StatusCode -ge 200) { exit 0 } else { exit 1 } } catch { exit 1 }" >nul 2>nul
if errorlevel 1 (
  start "Shiroha Quiz Local Server" /min cmd /k "cd /d ""%~dp0"" && %PYTHON_CMD% -m http.server %PORT% --bind 127.0.0.1"
  timeout /t 2 /nobreak >nul
)

start "" "%URL%"
