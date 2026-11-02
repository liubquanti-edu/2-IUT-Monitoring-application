@echo off
setlocal

set "PROJECT_DIR=%~dp0"
set "JDK_ROOT=%USERPROFILE%\portable-jdk21"
set "JAVA_HOME="

if not exist "%JDK_ROOT%" (
    mkdir "%JDK_ROOT%"
)

for /d %%D in ("%JDK_ROOT%\jdk-*") do if not defined JAVA_HOME set "JAVA_HOME=%%~fD"

if not defined JAVA_HOME if exist "%JDK_ROOT%\bin\java.exe" set "JAVA_HOME=%JDK_ROOT%"

if not defined JAVA_HOME (
    echo Portable JDK 21 was not found. Downloading it now...
    powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference = 'Stop'; $root = '%JDK_ROOT%'; $zip = Join-Path $env:TEMP 'netmonitor-jdk21.zip'; $url = 'https://api.adoptium.net/v3/binary/latest/21/ga/windows/x64/jdk/hotspot/normal/eclipse'; Invoke-WebRequest -Uri $url -OutFile $zip; Expand-Archive -Path $zip -DestinationPath $root -Force; Remove-Item $zip -Force"
    if errorlevel 1 (
        echo.
        echo Could not download the portable JDK 21.
        echo Check your internet connection or download it manually from:
        echo   https://adoptium.net/temurin/releases/
        pause
        exit /b 1
    )
    for /d %%D in ("%JDK_ROOT%\jdk-*") do if not defined JAVA_HOME set "JAVA_HOME=%%~fD"
)

if not exist "%JAVA_HOME%\bin\java.exe" (
    echo The selected JDK is invalid:
    echo   %JAVA_HOME%
    pause
    exit /b 1
)

set "PATH=%JAVA_HOME%\bin;%PATH%"
echo Using Java:
"%JAVA_HOME%\bin\java.exe" -version
echo.

echo Starting NetMonitor...
call "%PROJECT_DIR%mvnw.cmd" -f "%PROJECT_DIR%pom.xml" javafx:run
set "EXIT_CODE=%ERRORLEVEL%"

if not "%EXIT_CODE%" == "0" (
    echo.
    echo NetMonitor stopped with exit code %EXIT_CODE%.
    pause
)

endlocal & exit /b %EXIT_CODE%
