@ECHO OFF
REM mystic-crypt command-line interface launcher (Windows). Runs the CLI entry point from the same
REM application jar the desktop app uses (it bundles the mystic-crypt library, which carries the CLI).
REM Example: mystic-crypt.bat kem --algorithm hybrid
REM The flags are the ones of start.sh and start.bat (--add-opens and --sun-misc-unsafe-memory-access),
REM written once more here because a batch file cannot read them from there. The exit code is the command's.
java --add-opens java.base/java.time=ALL-UNNAMED --sun-misc-unsafe-memory-access=allow -cp "%~dp0mystic-crypt-ui.jar" io.github.astrapi69.mystic.crypt.cli.MysticCryptCli %*
EXIT /B %ERRORLEVEL%
