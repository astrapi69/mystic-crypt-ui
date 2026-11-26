@ECHO OFF
REM Starts the application without a console window. The Java is the one find-java.bat finds, the way
REM start.sh finds it with find-java.sh: the one the installer ran on, then the PATH's, 25 or newer (#1195).
REM A start from a menu or a double click has no terminal, so when none fits the reason is shown in a dialog
REM as well as written to standard error. msg.exe, the usual batch way to show text, is out: it sends to the
REM sessions of a Remote Desktop Session Host (Microsoft Learn, msg), not to the local user. A Windows Forms
REM message box through powershell.exe, which every Windows from 10 on carries, is a modal dialog
REM (Microsoft Learn, MessageBox.Show). The text goes through the environment, never into the command.
REM The flags are those of find-java.sh (--add-opens and --sun-misc-unsafe-memory-access), written out here
REM once more because a batch file cannot read them from there.
SETLOCAL
CALL "%~dp0find-java.bat"
IF NOT ERRORLEVEL 1 GOTO run
SETLOCAL EnableDelayedExpansion
SET "MYSTIC_CRYPT_MESSAGE=mystic-crypt-ui needs Java 25 or newer and cannot start. Tried:!JAVA_REPORT! Install a Java 25 JDK and start it again."
>&2 ECHO !MYSTIC_CRYPT_MESSAGE!
powershell -NoProfile -NonInteractive -WindowStyle Hidden -Command "Add-Type -AssemblyName System.Windows.Forms; [void][System.Windows.Forms.MessageBox]::Show($env:MYSTIC_CRYPT_MESSAGE, 'mystic-crypt-ui')"
EXIT /B 1
:run
start "" "%FOUND_JAVA_DIR%javaw.exe" --add-opens java.base/java.time=ALL-UNNAMED --sun-misc-unsafe-memory-access=allow -jar "%~dp0mystic-crypt-ui.jar"
