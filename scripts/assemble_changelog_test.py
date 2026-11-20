#!/usr/bin/env python3
"""Fixture tests for assemble_changelog.py. Run: python3 scripts/assemble_changelog_test.py"""

import json
import unittest
from pathlib import Path
from subprocess import CompletedProcess
from tempfile import TemporaryDirectory

from assemble_changelog import (advisory_published, assemble, insert, parse_name,
                                unmerged_fragments)

ADVISORY = "GHSA-abcd-efgh-2345"

NOT_FOUND = ('{"message":"Not Found","documentation_url":"https://docs.github.com/rest/'
             'security-advisories/global-advisories#get-a-global-security-advisory","status":"404"}')

JETTISON_ADVISORIES = {"GHSA-x27m-9w8j-5vcw": "2022-09-17T00:00:41Z",
                       "GHSA-grr4-wv38-f68w": "2022-12-13T15:30:27Z",
                       "GHSA-7rf3-mqpx-h7xg": "2022-12-13T15:30:26Z",
                       "GHSA-q6g2-g7f3-rr83": "2023-03-22T06:30:21Z"}


def fragment(directory, name, text):
    path = Path(directory) / name
    path.write_text(text, encoding="utf-8")
    return path


def global_advisory(advisory_id=ADVISORY):
    return f"advisories/{advisory_id}"


def repository_advisory(repository, advisory_id=ADVISORY):
    return f"repos/astrapi69/{repository}/security-advisories/{advisory_id}"


def gh_answering(advisories):
    """Stands in for subprocess.run and answers gh api as gh does (measured 2026-11-20): a known
    path's advisory as JSON with exit 0, any other path a 404, whose error body gh prints on
    standard output, with exit 1."""
    def run(command, **options):
        path = command[2]
        if path in advisories:
            return CompletedProcess(command, 0, json.dumps(advisories[path]), "")
        return CompletedProcess(command, 1, NOT_FOUND, "gh: Not Found (HTTP 404)\n")
    return run


def gh_offline(command, **options):
    """gh without a connection (measured 2026-11-20): nothing on standard output, exit 1."""
    return CompletedProcess(command, 1, "", "dial tcp: connect: connection refused\n")


class AssembleTest(unittest.TestCase):

    def test_sections_come_in_the_files_order_with_upper_case_headings(self):
        with TemporaryDirectory() as directory:
            fragments = [
                fragment(directory, "900.fixed.md", "- a save keeps its backup (#847)"),
                fragment(directory, "901.added.md", "- a release key plugin (#644)"),
                fragment(directory, "902.security.md", "- jsoup 1.23.2 instead of 1.22.2 (#688)"),
            ]
            block = assemble("8.15", fragments)
        self.assertEqual("""Version 8.15
-------------

SECURITY:
- jsoup 1.23.2 instead of 1.22.2 (#688)

ADDED:
- a release key plugin (#644)

FIXED:
- a save keeps its backup (#847)
""", block)

    def test_a_multi_line_bullet_keeps_its_continuation_lines(self):
        with TemporaryDirectory() as directory:
            fragments = [fragment(directory, "900.changed.md",
                                  "- the first line\n  and its continuation (#1)")]
            block = assemble("8.15", fragments)
        self.assertIn("- the first line\n  and its continuation (#1)", block)

    def test_an_unpublished_advisory_id_in_a_fragment_is_refused(self):
        with TemporaryDirectory() as directory:
            fragments = [fragment(directory, "900.security.md",
                                  "- a hole in the reader (GHSA-abcd-efgh-2345)")]
            with self.assertRaises(SystemExit) as refused:
                assemble("8.16", fragments, lambda advisory_id: False)
        self.assertIn("GHSA-abcd-efgh-2345", str(refused.exception))

    def test_the_default_refuses_every_advisory_id(self):
        with TemporaryDirectory() as directory:
            fragments = [fragment(directory, "900.security.md", "- x (GHSA-abcd-efgh-2345)")]
            with self.assertRaises(SystemExit):
                assemble("8.16", fragments)

    def test_a_published_advisory_id_in_a_fragment_is_kept(self):
        with TemporaryDirectory() as directory:
            fragments = [fragment(directory, "900.security.md", "- x (ghsa-abcd-efgh-2345)")]
            block = assemble("8.16", fragments, lambda advisory_id: True)
        self.assertIn("ghsa-abcd-efgh-2345", block)

    def test_a_fragment_naming_advisories_of_the_global_database_is_kept(self):
        """The 8.14 SECURITY block named the four advisories of jettison (CHANGELOG.md, #684)."""
        answers = gh_answering({global_advisory(advisory_id): {"state": None,
                                                               "published_at": published_at}
                                for advisory_id, published_at in JETTISON_ADVISORIES.items()})
        with TemporaryDirectory() as directory:
            fragments = [fragment(directory, "684.security.md",
                                  "- jettison 1.5.7 instead of 1.5.1 in the installer (#684), 1.5.1"
                                  " has four high advisories (" + ", ".join(JETTISON_ADVISORIES)
                                  + ")")]
            block = assemble("8.16", fragments,
                             lambda advisory_id: advisory_published(advisory_id, answers))
        self.assertIn("four high advisories (" + ", ".join(JETTISON_ADVISORIES) + ")", block)

    def test_the_neutral_security_line_without_an_id_passes(self):
        with TemporaryDirectory() as directory:
            fragments = [fragment(directory, "900.security.md",
                                  "- Fixes a security issue; its advisory follows after the release.")]
            block = assemble("8.16", fragments)
        self.assertIn("SECURITY:\n- Fixes a security issue", block)

    def test_a_fragment_without_a_bullet_is_refused(self):
        with TemporaryDirectory() as directory:
            fragments = [fragment(directory, "900.fixed.md", "no bullet")]
            with self.assertRaises(SystemExit):
                assemble("8.15", fragments)

    def test_a_wrong_name_is_refused(self):
        for name in ("x.fixed.md", "900.misc.md", "900.fixed.txt", "900.md"):
            with TemporaryDirectory() as directory:
                with self.assertRaises(SystemExit):
                    parse_name(fragment(directory, name, "- a"))

    def test_a_fragment_of_an_unmerged_pull_request_is_named(self):
        with TemporaryDirectory() as directory:
            fragments = [fragment(directory, "900.fixed.md", "- merged"),
                         fragment(directory, "901.added.md", "- open")]
            strays = unmerged_fragments(fragments,
                                        lambda number: "MERGED" if number == "900" else "OPEN")
        self.assertEqual(["901.added.md"], [path.name for path in strays])

    def test_every_fragment_merged_means_no_strays(self):
        with TemporaryDirectory() as directory:
            fragments = [fragment(directory, "900.fixed.md", "- merged")]
            self.assertEqual([], unmerged_fragments(fragments, lambda number: "MERGED"))

    def test_insert_puts_the_block_above_the_previous_version(self):
        changelog = ("## Change log\n----------------------\n\n"
                     "Version 8.14\n-------------\n\nFIXED:\n- old\n")
        result = insert(changelog, "Version 8.15\n-------------\n\nFIXED:\n- new\n")
        self.assertTrue(result.index("Version 8.15") < result.index("Version 8.14"))
        self.assertTrue(result.startswith("## Change log\n----------------------\n\nVersion 8.15"))

    def test_insert_refuses_an_unknown_heading(self):
        with self.assertRaises(SystemExit):
            insert("# Something else\n", "Version 8.15\n")


class AdvisoryPublishedTest(unittest.TestCase):

    def test_which_advisories_count_as_published(self):
        cases = (
            ("an entry of the global database with its publication date",
             gh_answering({global_advisory(): {"state": None,
                                               "published_at": "2022-12-13T15:30:26Z"}}), True),
            ("a published advisory of randomizer",
             gh_answering({repository_advisory("randomizer"): {
                 "state": "published", "published_at": "2026-10-30T13:21:21Z"}}), True),
            ("a published advisory of mystic-crypt-ui",
             gh_answering({repository_advisory("mystic-crypt-ui"): {
                 "state": "published", "published_at": "2026-11-18T07:41:08Z"}}), True),
            ("a draft of a family repository",
             gh_answering({repository_advisory("mystic-crypt"): {"state": "draft",
                                                                 "published_at": None}}), False),
            ("an entry of the global database without a publication date",
             gh_answering({global_advisory(): {"state": None, "published_at": None}}), False),
            ("an id nobody knows, a 404 everywhere", gh_answering({}), False),
            ("gh without a connection", gh_offline, False),
        )
        for name, run, published in cases:
            with self.subTest(name):
                self.assertEqual(published, advisory_published(ADVISORY, run))


if __name__ == "__main__":
    unittest.main(verbosity=1)
