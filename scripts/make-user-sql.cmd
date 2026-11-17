@echo off
rem scripts/make-user-sql.cmd
rem Local-only utility: prints a users-table INSERT with a freshly generated bcrypt hash.
rem No password is written to disk, logged, or committed by this script.
rem Run interactively to get hidden password input.

cd /d "%~dp0"
setlocal

set M2_REPO=%USERPROFILE%\.m2\repository\org\springframework\security\spring-security-crypto\7.1.2
set JAR=%M2_REPO%\spring-security-crypto-7.1.2.jar
set CL_REPO=%USERPROFILE%\.m2\repository\commons-logging\commons-logging\1.3.6
set CL_JAR=%CL_REPO%\commons-logging-1.3.6.jar

if not exist "%JAR%" (
  echo spring-security-crypto-7.1.2.jar not found at %M2_REPO%
  echo Run the backend build once: cd backend and mvnw.cmd clean package -DskipTests
  exit /b 1
)
if not exist "%CL_JAR%" (
  echo commons-logging-1.3.6.jar not found at %CL_REPO%
  echo Run the backend build once: cd backend and mvnw.cmd clean package -DskipTests
  exit /b 1
)

set OUT_DIR=%TEMP%\make-user-sql
if exist "%OUT_DIR%\MakeUser.class" del /f /q "%OUT_DIR%\MakeUser.class" >nul
javac -cp "%JAR%" -d "%OUT_DIR%" MakeUser.java || exit /b 1

java -cp "%OUT_DIR%;%JAR%;%CL_JAR%" MakeUser
exit /b %ERRORLEVEL%
