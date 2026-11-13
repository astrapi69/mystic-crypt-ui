#!/usr/bin/env bash
#
# Mirrors the wiki's source in this repository into the GitHub wiki, which is a git repository of
# its own (<repository>.wiki.git). Run by .github/workflows/wiki-mirror.yml; usage:
#
#   scripts/mirror-wiki.sh <source directory> <wiki remote>
#
# The wiki gets exactly the pages of the source directory, its *.md files: a page new or changed
# there is written, a page it no longer has is deleted, an unchanged page is not touched. What in
# the wiki is not a page - a .gitignore, an uploaded image - stays as it is. Every page written or
# deleted is named, and a last line counts them (#869).
#
# It fails closed: a source directory without a page is refused before anything is cloned, because
# mirroring nothing would delete every page of the wiki, and a wiki that cannot be cloned or refuses
# the push fails the run instead of reporting a mirror that did not happen.
#
# The commit is made with the identity git finds in the environment (GIT_AUTHOR_NAME and the
# others); the workflow sets the person who pushed or started it.

set -euo pipefail

if [ "$#" -ne 2 ]; then
  echo "usage: $0 <source directory> <wiki remote>" >&2
  exit 2
fi

source_dir="$1"
wiki_remote="$2"

if [ ! -d "$source_dir" ]; then
  echo "no source directory $source_dir: nothing mirrored" >&2
  exit 1
fi
source_dir="$(cd "$source_dir" && pwd)"

shopt -s nullglob
source_pages=("$source_dir"/*.md)
if [ "${#source_pages[@]}" -eq 0 ]; then
  echo "no page (*.md) in $source_dir: refusing to mirror, it would delete every page of the wiki" >&2
  exit 1
fi

clone="$(mktemp -d)"
trap 'rm -rf "$clone"' EXIT

if ! git clone -q "$wiki_remote" "$clone"; then
  echo "cannot clone the wiki $wiki_remote: not written" >&2
  exit 1
fi

written=0
deleted=0
cd "$clone"

# A wiki page name may carry a colon (the Help:- pages released versions link, #950), which is
# invalid in a file name on Windows, where CI checks this repository out. The source carries such a
# name with %3A, and the mirror maps it: %3A in a source file name becomes : in the wiki page name,
# and a wiki page with : is owned by the source file with %3A.
for page in *.md; do
  if [ ! -f "$source_dir/$page" ] && [ ! -f "$source_dir/${page//:/%3A}" ]; then
    git rm -q -- "$page"
    echo "deleted: $page"
    deleted=$((deleted + 1))
  fi
done

for source_page in "${source_pages[@]}"; do
  page="$(basename "$source_page")"
  page="${page//%3A/:}"
  if ! cmp -s "$source_page" "$page"; then
    cp "$source_page" "$page"
    git add -- "$page"
    echo "written: $page"
    written=$((written + 1))
  fi
done

summary="$written written, $deleted deleted, ${#source_pages[@]} pages in $source_dir"

if git diff --cached --quiet; then
  echo "the wiki already holds these pages: $summary"
  exit 0
fi

git commit -q -m "Mirror the wiki's source: $written written, $deleted deleted"
if ! git push -q origin HEAD; then
  echo "the wiki refused the push: not written ($summary)" >&2
  exit 1
fi
echo "the wiki is mirrored: $summary"
