#!/bin/bash
# Creates and removes the symlink through which a terminal finds the command-line starter,
# ~/.local/bin/mystic-crypt on Linux and macOS. The installer runs it with "link" once the
# commandline pack is written, the uninstaller with "unlink" before it deletes the installation;
# IzPack has no element for a symlink of its own (install.xml, pack "commandline").
#
#   mystic-crypt-link.sh link|unlink <starter> <link>
#
# link creates the link's directory when it is missing, then the link. A file of the same name is
# left alone, unless it is a symlink whose target is gone or the starter of another installation
# of this application (a mystic-crypt beside a mystic-crypt-ui.jar), which it replaces. unlink
# removes the link only when it points at this starter. What was left alone is named on standard
# error. The exit code is 0, unless the directory or the link could not be written (1) or the
# arguments are wrong (2).

action=${1:-}
starter=${2:-}
link=${3:-}

usage() {
  echo "usage: ${0##*/} link|unlink <starter> <link>" >&2
  exit 2
}

points_at_the_starter() {
  [ -L "$link" ] && [ "$(readlink "$link")" = "$starter" ]
}

# a symlink whose target is gone, or one to the starter of another installation of this application
replaceable() {
  local target
  [ -L "$link" ] || return 1
  [ -e "$link" ] || return 0
  target=$(readlink "$link")
  case $target in
    /*) ;;
    *) target="${link%/*}/$target" ;;
  esac
  [ "${target##*/}" = mystic-crypt ] && [ -f "${target%/*}/mystic-crypt-ui.jar" ]
}

create_link() {
  if points_at_the_starter; then
    echo "$link already points at $starter"
    return 0
  fi
  if [ -e "$link" ] || [ -L "$link" ]; then
    if ! replaceable; then
      echo "$link exists and is no link to an installation of mystic-crypt-ui, left as it is;" \
        "call $starter by its full path" >&2
      return 0
    fi
    rm -f "$link" || return 1
  fi
  mkdir -p "${link%/*}" || return 1
  ln -s "$starter" "$link" || return 1
  echo "$link -> $starter"
}

remove_link() {
  if points_at_the_starter; then
    rm -f "$link" || return 1
    echo "removed $link"
  elif [ -e "$link" ] || [ -L "$link" ]; then
    echo "$link does not point at $starter, left as it is" >&2
  fi
  return 0
}

[ -n "$starter" ] && [ -n "$link" ] || usage
case $action in
  link) create_link ;;
  unlink) remove_link ;;
  *) usage ;;
esac
