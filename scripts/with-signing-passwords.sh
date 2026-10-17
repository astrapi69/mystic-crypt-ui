#!/usr/bin/env bash
#
# Runs one command with the release keystore's two passwords in its environment, read from the
# desktop's keyring, and in no other process (#579): not on the command line, where the process list
# shows every argument, and not in a file. gradle/packaging.gradle reads them as
# MYSTIC_CRYPT_STORE_PASSWORD and MYSTIC_CRYPT_KEY_PASSWORD when it signs the jar.
#
#   scripts/with-signing-passwords.sh <command> [arguments...]
#
# The passwords are stored in the keyring once, each read from the terminal:
#
#   secret-tool store --label='mystic-crypt-ui release keystore' service mystic-crypt-ui purpose store-password
#   secret-tool store --label='mystic-crypt-ui release key' service mystic-crypt-ui purpose key-password
#
# Without secret-tool, or without the two entries, the command runs without the variables, and the
# build falls back to the release.mystic-crypt.* properties - or refuses to sign when those are gone
# too. The script says which way it went; it never prints a value.
set -euo pipefail

[ "$#" -gt 0 ] || { echo "usage: scripts/with-signing-passwords.sh <command> [arguments...]" >&2; exit 2; }

say() { printf '==> %s\n' "$*"; }
# The lookup always reads the GNOME keyring (#595). A process started from a snap - VS Code is one -
# carries the snap's SNAP* variables, and libsecret then takes it for sandboxed and reads its own file
# store, ~/.local/share/keyrings/default.keyring, which Seahorse does not show and a terminal does not
# read. So the lookup runs in a subshell without them; the command keeps its environment.
lookup() {
  (
    for name in $(compgen -e); do
      [[ "$name" == SNAP* ]] && unset "$name"
    done
    secret-tool lookup service mystic-crypt-ui purpose "$1" 2>/dev/null
  )
}

if ! command -v secret-tool >/dev/null 2>&1; then
  say "secret-tool is not installed (package libsecret-tools): the signing passwords are not read from the keyring"
  exec "$@"
fi
if store="$(lookup store-password)" && key="$(lookup key-password)" && [ -n "$store" ] && [ -n "$key" ]; then
  say "the signing passwords come from the keyring"
  export MYSTIC_CRYPT_STORE_PASSWORD="$store" MYSTIC_CRYPT_KEY_PASSWORD="$key"
  exec "$@"
fi
say "the keyring holds no signing passwords for mystic-crypt-ui: they are not read from the keyring"
exec "$@"
