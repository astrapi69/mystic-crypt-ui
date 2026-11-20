#!/usr/bin/env python3
"""Assembles changelog.d/ fragments into CHANGELOG.md for a release, then deletes them.

The release pull request runs it once (squash pilot, 2026-11-11): the fragments become the
new version's block, in the section order and shape CHANGELOG.md already uses; nothing else
in the file is touched, and the intro sentence under the version heading stays the release
pull request's manual work. Standard library only - towncrier would bring a package, a
pyproject and a template for a file shape this script reproduces in a few lines (AG-103).

Usage: scripts/assemble_changelog.py <version> [--dry-run]
  --dry-run prints the assembled block and deletes nothing.
"""

import json
import re
import subprocess
import sys
from pathlib import Path

ADVISORY_ID = re.compile(r"GHSA-[0-9a-z]{4}-[0-9a-z]{4}-[0-9a-z]{4}", re.IGNORECASE)

ADVISORY_REPOSITORIES = ("mystic-crypt-ui", "mystic-crypt", "crypt-data", "crypt-api")

SECTION_ORDER = ("security", "added", "changed", "fixed", "deprecated", "removed")


def fragment_files(directory):
    """Every fragment in the directory, README aside, sorted by number then section."""
    return sorted(path for path in directory.glob("*.md") if path.name != "README.md")


def parse_name(path):
    """<number>.<section>.md -> (number, section); refuses another shape loudly."""
    parts = path.name.split(".")
    if len(parts) != 3 or parts[2] != "md" or parts[1] not in SECTION_ORDER \
            or not parts[0].isdigit():
        raise SystemExit(f"assemble-changelog: {path} is not <pull request number>.<section>.md"
                         f" with a section of {', '.join(SECTION_ORDER)}")
    return parts[0], parts[1]


def unmerged_fragments(fragments, state_of):
    """The fragments whose pull request is not merged: a fragment travels in its pull
    request, so one lying here while the pull request is open (or closed unmerged) is a
    mistake the release must not swallow (decided 2026-11-11). ``state_of`` maps a pull
    request number to its state."""
    return [path for path in fragments if state_of(parse_name(path)[0]) != "MERGED"]


def pull_request_state(number):
    """The state gh reports for the pull request, or UNKNOWN when gh cannot say."""
    completed = subprocess.run(["gh", "pr", "view", number, "--json", "state"],
                               capture_output=True, text=True)
    if completed.returncode != 0:
        return "UNKNOWN"
    return json.loads(completed.stdout).get("state", "UNKNOWN")


def gh_advisory(path, run=subprocess.run):
    """The state and the publication date gh api reports for the advisory at the path, or an
    empty dict when the call fails. On a 404 gh prints its error body on standard output and
    exits 1, so the exit code decides, never the output."""
    completed = run(["gh", "api", path, "--jq", "{state, published_at}"],
                    capture_output=True, text=True)
    return json.loads(completed.stdout) if completed.returncode == 0 else {}


def advisory_published(advisory_id, run=subprocess.run):
    """True when one of the family's repositories reports the advisory as published. The
    global advisory database does not list these ids, so the repositories' own advisories are
    asked; an unpublished advisory, an unknown id and a failed call all answer False (fail
    closed). ``run`` is subprocess.run, a parameter so that a test can answer for gh."""
    return any(gh_advisory(f"repos/astrapi69/{repository}/security-advisories/{advisory_id}",
                           run).get("state") == "published"
               for repository in ADVISORY_REPOSITORIES)


def unpublished_advisory_ids(text, is_published):
    """The advisory ids in the text whose advisory is not published yet (AG-117)."""
    return sorted({found for found in ADVISORY_ID.findall(text) if not is_published(found)})


def assemble(version, fragments, is_published=lambda advisory_id: False):
    """The new version's block, fragments grouped in the file's section order. A fragment that
    names an advisory not yet published is refused: the release pull request carries the neutral
    SECURITY line instead (AG-117, release-workflow.md step 3, #1021). ``is_published`` maps an
    advisory id to whether it is published; the default refuses every id."""
    by_section = {}
    for path in fragments:
        _, section = parse_name(path)
        bullet = path.read_text(encoding="utf-8").strip()
        unpublished = unpublished_advisory_ids(bullet, is_published)
        if unpublished:
            raise SystemExit(f"assemble-changelog: {path} names an advisory that is not published: "
                             + ", ".join(unpublished) + " - the release pull request carries the"
                             " neutral SECURITY line without an id (AG-117)")
        if not bullet.startswith("- "):
            raise SystemExit(f"assemble-changelog: {path} does not start with a bullet ('- ')")
        by_section.setdefault(section, []).append(bullet)
    lines = [f"Version {version}", "-------------", ""]
    for section in SECTION_ORDER:
        if section not in by_section:
            continue
        lines.append(section.upper() + ":")
        lines.extend(by_section[section])
        lines.append("")
    return "\n".join(lines)


def insert(changelog_text, block):
    """The block goes under the file heading, above the previous version."""
    heading = "## Change log\n----------------------\n\n"
    if not changelog_text.startswith(heading):
        raise SystemExit("assemble-changelog: CHANGELOG.md does not start with the known heading"
                         " - not touching it")
    return heading + block + "\n" + changelog_text[len(heading):]


def main(argv):
    if not argv or len(argv) > 2 or (len(argv) == 2 and argv[1] != "--dry-run"):
        raise SystemExit("usage: scripts/assemble_changelog.py <version> [--dry-run]")
    version = argv[0]
    dry_run = len(argv) == 2
    repository = Path(__file__).resolve().parent.parent
    directory = repository / "changelog.d"
    fragments = fragment_files(directory)
    if not fragments:
        raise SystemExit("assemble-changelog: no fragments in changelog.d/ - a release without"
                         " user-visible changes states that by hand, this script has nothing to do")
    strays = unmerged_fragments(fragments, pull_request_state)
    if strays:
        raise SystemExit("assemble-changelog: fragments of unmerged pull requests - a fragment"
                         " travels in its pull request, move these back or merge first: "
                         + ", ".join(path.name for path in strays))
    block = assemble(version, fragments, advisory_published)
    if dry_run:
        print(block)
        print(f"(dry run: {len(fragments)} fragment(s) read, nothing written, nothing deleted)")
        return
    changelog = repository / "CHANGELOG.md"
    changelog.write_text(insert(changelog.read_text(encoding="utf-8"), block), encoding="utf-8")
    for path in fragments:
        path.unlink()
    print(f"CHANGELOG.md carries Version {version} from {len(fragments)} fragment(s);"
          " the fragments are deleted - commit both in the release pull request")


if __name__ == "__main__":
    main(sys.argv[1:])
