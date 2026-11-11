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

import sys
from pathlib import Path

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


def assemble(version, fragments):
    """The new version's block, fragments grouped in the file's section order."""
    by_section = {}
    for path in fragments:
        _, section = parse_name(path)
        bullet = path.read_text(encoding="utf-8").strip()
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
    block = assemble(version, fragments)
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
