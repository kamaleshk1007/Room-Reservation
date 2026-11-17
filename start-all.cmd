@echo off
rem Room Reserve - start everything: MySQL check, dummy portal, backend, frontend.
rem No secrets in this script. Each Spring service auto-loads its own .env from its
rem working directory (spring.config.import in application.properties); the Vite
rem frontend loads frontend/.env automatically. Run from a normal cmd window.

cd /d "%~dp0"
setlocal EnableDelayedExpansion

echo === Room Reserve: starting services ===

echo.
echo [1/4] Checking MySQL97...
sc query "MySQL97" | findstr /C:"RUNNING" >nul 2>&1
if %errorlevel%==0 (
  echo   MySQL97 is running.
  goto mysql_ok
)
echo   MySQL97 is not running; trying 'net start MySQL97'...
net start MySQL97 >nul 2>&1
sc query "MySQL97" | findstr /C:"RUNNING" >nul 2>&1
if %errorlevel%==0 (
  echo   MySQL97 started.
  goto mysql_ok
)
netstat -ano | findstr ":3306" | findstr "LISTENING" >nul 2>&1
if %errorlevel%==0 (
  echo   Service could not be opened, but MySQL is listening on 3306 (elevated mysqld).
  goto mysql_ok
)
echo   FATAL: MySQL is not running and cannot be started from here.
echo   Start mysqld elevated first (or run this script from an elevated prompt):
echo   Start-Process "C:\Program Files\MySQL\MySQL Server 9.7\bin\mysqld.exe" -ArgumentList "--defaults-file=C:\ProgramData\MySQL\MySQL Server 9.7\my.ini" -Verb RunAs
exit /b 1

:mysql_ok

echo.
echo [2/4] Starting dummy portal on 8090 (loads dummy-portal/.env)...
start "dummy portal (8090)" cmd /k "cd /d %~dp0dummy-portal && mvnw.cmd spring-boot:run"
call :waitport 8090 "http://localhost:8090/" 90 "dummy portal"
if not %waitresult%==200 (
  echo   dummy portal FAILED to become healthy.
  set PORTAL_OK=0
) else (
  set PORTAL_OK=1
  echo   dummy portal healthy.
)

echo.
echo [3/4] Starting Room Reserve backend on 8080 (loads backend/.env)...
start "Room Reserve backend (8080)" cmd /k "cd /d %~dp0backend && mvnw.cmd spring-boot:run"
call :waitport 8080 "http://localhost:8080/actuator/health" 90 "backend"
if not %waitresult%==200 (
  echo   backend FAILED to become healthy.
  set BACKEND_OK=0
) else (
  set BACKEND_OK=1
  echo   backend healthy.
)

echo.
echo [4/4] Starting frontend on 5173 (loads frontend/.env)...
start "Room Reserve frontend (5173)" cmd /k "cd /d %~dp0frontend && pnpm dev"
call :waitport 5173 "http://localhost:5173/" 90 "frontend"
if not %waitresult%==200 (
  echo   frontend FAILED to become healthy.
  set FRONTEND_OK=0
) else (
  set FRONTEND_OK=1
  echo   frontend healthy.
)

echo.
echo ==================================================
echo Room Reserve startup summary
echo ==================================================
if %PORTAL_OK%==1 (echo   PASS  dummy portal  http://localhost:8090) else (echo   FAIL  dummy portal  http://localhost:8090)
if %BACKEND_OK%==1 (echo   PASS  backend       http://localhost:8080) else (echo   FAIL  backend       http://localhost:8080)
if %FRONTEND_OK%==1 (echo   PASS  frontend      http://localhost:5173) else (echo   FAIL  frontend      http://localhost:5173)
echo ==================================================
exit /b 0

:waitport
rem %1=port %2=health URL %3=attempts %4=name
rem PowerShell probe: prints StatusCode (newline-terminated, captured by for /f).
rem On connection refusal prints X. No shell-expansion pitfalls.
set waitresult=000
for /L %%i in (1,1,%3) do (
  set waitresult=000
  for /f %%h in ('powershell -NoProfile -Command "try { $r=Invoke-WebRequest -UseBasicParsing -Uri '%2' -TimeoutSec 3; Write-Output $r.StatusCode } catch { Write-Output 'X' }"') do set waitresult=%%h
  if "!waitresult!"=="200" goto :waitport_done
  timeout /t 2 /nobreak >nul
)
:waitport_done
exit /b 0
