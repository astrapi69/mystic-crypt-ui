@ECHO OFF
REM Called by start.bat and mystic-crypt.bat, never run on its own: the one place that says which Java the
REM Windows starters run on (#1195), the batch counterpart of find-java.sh with the same order and the same
REM minimum. The Java found by name may be older than the application needs, which fails without a word, so
REM the order is the Java the installer ran on (the file installer-java-home, which the installer writes
REM next to this file), then the java.exe on the PATH, and a candidate counts when its major version, read
REM from java -version, is MINIMUM_JAVA or newer.
REM
REM Sets FOUND_JAVA to the java.exe it picked and FOUND_JAVA_DIR to its directory (with the trailing
REM backslash) and returns 0, or leaves what it tried in JAVA_REPORT, one bracket per candidate, and returns 1.
REM What a starter does with a failure is its own: start.bat shows a dialog, mystic-crypt.bat writes to
REM standard error only. The JVM flags are in the starters. The 1.x version scheme of Java 8 and older reads as 1.
SET "MINIMUM_JAVA=25"
SET "FOUND_JAVA="
SET "FOUND_JAVA_DIR="
SET "JAVA_REPORT="
SET "INSTALLER_JAVA_HOME="
IF EXIST "%~dp0installer-java-home" SET /P INSTALLER_JAVA_HOME=<"%~dp0installer-java-home"
IF DEFINED INSTALLER_JAVA_HOME CALL :try "%INSTALLER_JAVA_HOME%\bin\java.exe"
IF DEFINED FOUND_JAVA EXIT /B 0
SET "PATH_JAVA="
FOR %%I IN (java.exe) DO SET "PATH_JAVA=%%~$PATH:I"
IF DEFINED PATH_JAVA CALL :try "%PATH_JAVA%"
IF DEFINED FOUND_JAVA EXIT /B 0
IF NOT DEFINED JAVA_REPORT SET "JAVA_REPORT= [no java found, neither the installer's nor one on the PATH]"
EXIT /B 1

:try
SET "CANDIDATE=%~1"
IF NOT EXIST "%CANDIDATE%" GOTO notfound
SET "RAW_VERSION="
SET "MAJOR="
SET "VERSION_FILE=%TEMP%\mystic-crypt-java-%RANDOM%.txt"
"%CANDIDATE%" -version >"%VERSION_FILE%" 2>&1
FOR /F "usebackq tokens=1-3 delims= " %%A IN ("%VERSION_FILE%") DO IF /I "%%B"=="version" SET "RAW_VERSION=%%~C"
DEL "%VERSION_FILE%" 2>NUL
IF DEFINED RAW_VERSION FOR /F "delims=.-+_" %%M IN ("%RAW_VERSION%") DO SET "MAJOR=%%M"
IF NOT DEFINED MAJOR GOTO unknown
IF %MAJOR% GEQ %MINIMUM_JAVA% GOTO accept
SET "JAVA_REPORT=%JAVA_REPORT% [%CANDIDATE%: Java %MAJOR%]"
GOTO :EOF

:notfound
SET "JAVA_REPORT=%JAVA_REPORT% [%CANDIDATE%: not found]"
GOTO :EOF

:unknown
SET "JAVA_REPORT=%JAVA_REPORT% [%CANDIDATE%: Java unknown]"
GOTO :EOF

:accept
SET "FOUND_JAVA=%CANDIDATE%"
FOR %%I IN ("%CANDIDATE%") DO SET "FOUND_JAVA_DIR=%%~dpI"
GOTO :EOF
