#!/bin/bash
# The launcher the desktop entry runs: it finds a Java 25 or newer and starts the application with
# it. The menu has no terminal and a session PATH of its own, so the java it finds by name may be
# the system's older default, which fails without a word (#1009). The order is the Java the
# installer ran on (the file installer-java-home, which the installer writes), then the PATH's.
# When neither is 25 or newer the reason goes to standard error, to a dialog or notification where
# zenity or notify-send exist, and the exit code is non-zero.

MINIMUM_JAVA=25

# --sun-misc-unsafe-memory-access=allow: XStream, which reads and writes the database, asks once
# per start whether it may write fields through sun.misc.Unsafe by trying it. That method is
# deprecated for removal, so JDK 25 answers with a five line warning on every start. XStream falls
# back on its own when the call stops working, so this permits what it is going to do anyway.
JVM_FLAGS=(--add-opens java.base/java.time=ALL-UNNAMED --sun-misc-unsafe-memory-access=allow)

SELF="$(readlink -f "$0" 2>/dev/null || echo "$0")"
DIR="${SELF%/*}"

# prints the major version of the given java, nothing when it cannot be read; the 1.x scheme of
# Java 8 and older reads as 1
java_major() {
  local line
  while IFS= read -r line; do
    if [[ $line =~ version\ \"([0-9]+) ]]; then
      echo "${BASH_REMATCH[1]}"
      return
    fi
  done < <("$1" -version 2>&1)
}

installer_java() {
  local home=""
  [ -r "$DIR/installer-java-home" ] && IFS= read -r home < "$DIR/installer-java-home"
  [ -n "$home" ] && echo "$home/bin/java"
}

CANDIDATES=()
installer=$(installer_java)
[ -n "$installer" ] && CANDIDATES+=("$installer")
path_java=$(command -v java)
[ -n "$path_java" ] && CANDIDATES+=("$path_java")

report=""
for candidate in "${CANDIDATES[@]}"; do
  if [ ! -x "$candidate" ]; then
    report+=$'\n'"  $candidate: not found"
    continue
  fi
  major=$(java_major "$candidate")
  if [[ -n $major && $major -ge $MINIMUM_JAVA ]]; then
    exec "$candidate" "${JVM_FLAGS[@]}" -jar "$DIR/mystic-crypt-ui.jar" "$@"
  fi
  report+=$'\n'"  $candidate: Java ${major:-unknown}"
done

[ -z "$report" ] && report=$'\n'"  no java found, neither the installer's nor one on the PATH"
message="Mystic Crypt UI needs Java $MINIMUM_JAVA or newer and cannot start. Tried:$report"$'\n'"Install a Java $MINIMUM_JAVA JDK and start the application again."
echo "$message" >&2
if command -v zenity > /dev/null; then
  zenity --error --no-markup --title "Mystic Crypt UI" --text "$message"
elif command -v notify-send > /dev/null; then
  notify-send "Mystic Crypt UI" "$message"
fi
exit 1
