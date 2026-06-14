@echo off
setlocal

set "SCRIPT_DIR=%~dp0"
if "%SCRIPT_DIR:~-1%"=="\" set "SCRIPT_DIR=%SCRIPT_DIR:~0,-1%"

call "%SCRIPT_DIR%\gradlew.bat" -p "%SCRIPT_DIR%" --quiet installDist
set "BUILD_EXIT=%ERRORLEVEL%"
if not "%BUILD_EXIT%"=="0" exit /b %BUILD_EXIT%

call "%SCRIPT_DIR%\build\install\Dynamic-Reflector\bin\Dynamic-Reflector.bat" %*
exit /b %ERRORLEVEL%
