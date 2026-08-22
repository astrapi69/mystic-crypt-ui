@ECHO OFF
REM mystic-crypt command-line interface launcher (Windows). Runs the CLI entry point from the same
REM application jar the desktop app uses (it bundles the mystic-crypt library, which carries the CLI).
REM Example: mystic-crypt.bat kem --algorithm hybrid
java -cp "%~dp0mystic-crypt-ui.jar" io.github.astrapi69.mystic.crypt.cli.MysticCryptCli %*
