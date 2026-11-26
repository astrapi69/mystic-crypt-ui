@ECHO OFF
REM mystic-crypt command-line interface launcher (Windows). Runs the application jar's command line,
REM the path "java -jar mystic-crypt-ui.jar --cli" takes: the library's commands and the plugins'.
REM Example: mystic-crypt.bat kem --algorithm hybrid
REM The Java is the one find-java.bat finds, the way mystic-crypt finds it with find-java.sh: the one the
REM installer ran on, then the PATH's, 25 or newer. It goes first on the PATH of this run, so the java
REM below is that one. When none fits, the reason goes to standard error only, without a dialog, and the
REM exit code is 1. Arguments, standard input and the exit code of the command pass through.
REM The flags are those of find-java.sh (--add-opens and --sun-misc-unsafe-memory-access), written out here
REM once more, as in start.bat, because a batch file cannot read them from there.
SETLOCAL
CALL "%~dp0find-java.bat"
IF NOT ERRORLEVEL 1 GOTO run
SETLOCAL EnableDelayedExpansion
>&2 ECHO mystic-crypt needs Java 25 or newer and cannot run. Tried:!JAVA_REPORT!
>&2 ECHO Install a Java 25 JDK and run the command again.
EXIT /B 1
:run
SET "PATH=%FOUND_JAVA_DIR%;%PATH%"
java --add-opens java.base/java.time=ALL-UNNAMED --sun-misc-unsafe-memory-access=allow -jar "%~dp0mystic-crypt-ui.jar" --cli %*
EXIT /B %ERRORLEVEL%
