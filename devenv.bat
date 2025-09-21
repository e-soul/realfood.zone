@echo off
setlocal

set SCRIPT_DIR=%~dp0
if %SCRIPT_DIR:~-1%==\ set SCRIPT_DIR=%SCRIPT_DIR:~0,-1%

if not exist "%SCRIPT_DIR%\envvars.bat" (
	echo ERROR: "%SCRIPT_DIR%\envvars.bat" not found. Please create it with your local environment variables.
	exit /b 1
)

call "%SCRIPT_DIR%\envvars.bat"

if not defined GOOGLE_CLIENT_ID (
	echo ERROR: GOOGLE_CLIENT_ID is not set. Check envvars.bat.
	exit /b 1
)
if not defined GOOGLE_CLIENT_SECRET (
	echo ERROR: GOOGLE_CLIENT_SECRET is not set. Check envvars.bat.
	exit /b 1
)

set STATIC_BASE_URL=http://localhost:8050/static-content
set GOOGLE_REDIRECT_URI=http://localhost:8050/auth/google/callback

start "DynamoDB local" java -Djava.library.path="%SCRIPT_DIR%\dynamodb_local_latest\DynamoDBLocal_lib" -jar "%SCRIPT_DIR%\dynamodb_local_latest\DynamoDBLocal.jar" -disableTelemetry -sharedDb -port 5050
timeout /t 2 /nobreak >nul

start "HTTP server" java -cp "%SCRIPT_DIR%\zone.realfood.server\build\libs\zone.realfood.server.jar;%SCRIPT_DIR%\zone.realfood.backend\build\libs\zone.realfood.backend.jar" -Dzone.realfood.staticContentDir="%SCRIPT_DIR%\static-content" zone.realfood.Main

endlocal
