#!/usr/bin/env python3
"""Fixture tests for assemble_changelog.py. Run: python3 scripts/assemble_changelog_test.py"""

import unittest
from pathlib import Path
from tempfile import TemporaryDirectory

from assemble_changelog import assemble, insert, parse_name


def fragment(directory, name, text):
    path = Path(directory) / name
    path.write_text(text, encoding="utf-8")
    return path


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

    def test_insert_puts_the_block_above_the_previous_version(self):
        changelog = ("## Change log\n----------------------\n\n"
                     "Version 8.14\n-------------\n\nFIXED:\n- old\n")
        result = insert(changelog, "Version 8.15\n-------------\n\nFIXED:\n- new\n")
        self.assertTrue(result.index("Version 8.15") < result.index("Version 8.14"))
        self.assertTrue(result.startswith("## Change log\n----------------------\n\nVersion 8.15"))

    def test_insert_refuses_an_unknown_heading(self):
        with self.assertRaises(SystemExit):
            insert("# Something else\n", "Version 8.15\n")


if __name__ == "__main__":
    unittest.main(verbosity=1)
