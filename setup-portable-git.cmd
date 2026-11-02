@echo off
setlocal
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0setup-portable-git.ps1"
set "EXIT_CODE=%ERRORLEVEL%"
if not "%EXIT_CODE%" == "0" (
    echo.
    echo Portable Git setup failed with exit code %EXIT_CODE%.
    pause
)
endlocal & exit /b %EXIT_CODE%
