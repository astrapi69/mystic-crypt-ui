# Sourced by start.sh and mystic-crypt, never run on its own: the one place that says which Java the
# application runs on and with which flags (#1009). A menu start has no terminal and a session PATH
# of its own, and a terminal's PATH can hold an older default too, so the java found by name may be
# older than the application needs, which fails without a word. The order is the Java the
# installer ran on (the file installer-java-home, which the installer writes), then the PATH's.
#
# The starter sets DIR, its own resolved directory, before sourcing this. find_java then sets
# FOUND_JAVA to the first candidate that is Java MINIMUM_JAVA or newer and returns 0, or leaves
# what it tried in JAVA_REPORT, one line per candidate, and returns 1. What a starter does with a
# failure is its own: start.sh shows it, mystic-crypt writes it to standard error only.

MINIMUM_JAVA=25

# --sun-misc-unsafe-memory-access=allow: XStream, which reads and writes the database, asks once
# per start whether it may write fields through sun.misc.Unsafe by trying it. That method is
# deprecated for removal, so JDK 25 answers with a five line warning on every start. XStream falls
# back on its own when the call stops working, so this permits what it is going to do anyway.
JVM_FLAGS=(--add-opens java.base/java.time=ALL-UNNAMED --sun-misc-unsafe-memory-access=allow)

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

find_java() {
  local candidates=() installer path_java candidate major
  FOUND_JAVA=""
  JAVA_REPORT=""
  installer=$(installer_java)
  [ -n "$installer" ] && candidates+=("$installer")
  path_java=$(command -v java)
  [ -n "$path_java" ] && candidates+=("$path_java")
  for candidate in "${candidates[@]}"; do
    if [ ! -x "$candidate" ]; then
      JAVA_REPORT+=$'\n'"  $candidate: not found"
      continue
    fi
    major=$(java_major "$candidate")
    if [[ -n $major && $major -ge $MINIMUM_JAVA ]]; then
      FOUND_JAVA=$candidate
      return 0
    fi
    JAVA_REPORT+=$'\n'"  $candidate: Java ${major:-unknown}"
  done
  [ -z "$JAVA_REPORT" ] && JAVA_REPORT=$'\n'"  no java found, neither the installer's nor one on the PATH"
  return 1
}
