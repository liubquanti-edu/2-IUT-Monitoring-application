@echo off
rem Quick launch: starts MySQL + NetMonitor only (no Git, update, Claude Code or VS Code steps).
rem Run netmonitor.cmd once first so that MySQL and the JDK are installed.
call "%~dp0netmonitor.cmd" run
exit /b %ERRORLEVEL%
