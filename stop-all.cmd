@echo off
rem Room Reserve - stop the three app services (dummy portal, backend, frontend).
rem Stops by the PID bound to each port, so unrelated java/node processes are untouched.
rem MySQL is intentionally left running.

cd /d "%~dp0"
setlocal EnableDelayedExpansion

echo === Room Reserve: stopping services ===

for /L %%i in (1,1,3) do (
  if %%i==1 set PORT=8090& set NAME=dummy portal
  if %%i==2 set PORT=8080& set NAME=backend
  if %%i==3 set PORT=5173& set NAME=frontend

  set KILLED=0
  for /f "tokens=5" %%P in ('netstat -ano ^| findstr ":!PORT! " ^| findstr "LISTENING"') do (
    taskkill /F /PID %%P >nul 2>&1
    if !errorlevel! == 0 set KILLED=1
  )
  if !KILLED!==1 (echo   stopped !NAME! on port !PORT!) else (echo   nothing listening on port !PORT!: !NAME!)
)

echo.
echo Remaining listeners on 8090/8080/5173 (should be empty):
netstat -ano | findstr ":8090 :8080 :5173" | findstr "LISTENING" || echo   none.
exit /b 0
