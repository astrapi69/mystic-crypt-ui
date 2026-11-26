#!/bin/bash
# The launcher the desktop entry runs: it finds a Java 25 or newer and starts the application with
# it (#1009). Which Java and which flags is decided in find-java.sh, which mystic-crypt sources
# too. When no Java fits, the reason goes to standard error, to a dialog or notification where
# zenity or notify-send exist, and the exit code is non-zero.

SELF="$(readlink -f "$0" 2>/dev/null || echo "$0")"
DIR="${SELF%/*}"

if ! . "$DIR/find-java.sh"; then
  echo "Mystic Crypt UI cannot start: $DIR/find-java.sh is missing. Install the application again." >&2
  exit 1
fi

if find_java; then
  exec "$FOUND_JAVA" "${JVM_FLAGS[@]}" -jar "$DIR/mystic-crypt-ui.jar" "$@"
fi

message="Mystic Crypt UI needs Java $MINIMUM_JAVA or newer and cannot start. Tried:$JAVA_REPORT"$'\n'"Install a Java $MINIMUM_JAVA JDK and start the application again."
echo "$message" >&2
if command -v zenity > /dev/null; then
  zenity --error --no-markup --title "Mystic Crypt UI" --text "$message"
elif command -v notify-send > /dev/null; then
  notify-send "Mystic Crypt UI" "$message"
fi
exit 1
