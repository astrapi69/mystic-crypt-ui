@ECHO OFF
REM mystic-crypt command-line interface launcher (Windows). Runs the application jar's command line,
REM the path "java -jar mystic-crypt-ui.jar --cli" takes: the library's commands and the plugins'.
REM Example: mystic-crypt.bat kem --algorithm hybrid
REM The flags are the ones of start.sh and start.bat (--add-opens and --sun-misc-unsafe-memory-access),
REM written once more here because a batch file cannot read them from there. The exit code is the command's.
java --add-opens java.base/java.time=ALL-UNNAMED --sun-misc-unsafe-memory-access=allow -jar "%~dp0mystic-crypt-ui.jar" --cli %*
EXIT /B %ERRORLEVEL%
