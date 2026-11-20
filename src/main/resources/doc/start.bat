@ECHO OFF
REM The flags are the ones of start.sh (--add-opens and --sun-misc-unsafe-memory-access), written once more here because a batch file cannot read them from there.
start javaw --add-opens java.base/java.time=ALL-UNNAMED --sun-misc-unsafe-memory-access=allow -jar "%~dp0mystic-crypt-ui.jar"
