#!/usr/bin/env python3
"""Fixture tests for merge_pr.py, no GitHub involved (#867).

Run: python3 scripts/merge_pr_test.py

The acceptance cases of the issue (decided by the maintainer on 2026-11-11)
plus every guard the Bash script carried, encoded as a test. Red before
green: this suite expresses the PORT's required behaviour; against the Bash
script the NEUTRAL and SKIPPED cases fail (the measured defect of #863), the
run stands in the pull request.
"""

import unittest

from merge_pr import (RollupEntry, all_reported, base_mismatch, dirty_tree_note,
                      judge, merge_command, parse_rollup, resolve_required)

REQUIRED = ["build", "closing-references", "release-freeze"]


def green_required():
    return [RollupEntry(context, True, "SUCCESS") for context in REQUIRED]


class JudgeTest(unittest.TestCase):
    """The decision engine: the issue's cases 1-5 and the sharpened edges."""

    def test_case1_non_required_neutral_merges(self):
        entries = green_required() + [RollupEntry("Socket Security: Pull Request Alerts", True, "NEUTRAL")]
        verdict = judge(entries, REQUIRED)
        self.assertTrue(verdict.mergeable)
        self.assertIn("non-required, no verdict (not blocking): Socket Security: Pull Request Alerts (NEUTRAL)",
                      verdict.messages)

    def test_case2_non_required_skipped_merges(self):
        entries = green_required() + [RollupEntry("Socket Security: Pull Request Alerts", True, "SKIPPED")]
        self.assertTrue(judge(entries, REQUIRED).mergeable)

    def test_case3_non_required_failure_blocks_by_default(self):
        entries = green_required() + [RollupEntry("GitGuardian Security Checks", True, "FAILURE")]
        verdict = judge(entries, REQUIRED)
        self.assertFalse(verdict.mergeable)
        self.assertTrue(any("AG-090" in message for message in verdict.messages))

    def test_case3b_optional_failure_with_override_warns_and_merges(self):
        entries = green_required() + [RollupEntry("GitGuardian Security Checks", True, "FAILURE")]
        verdict = judge(entries, REQUIRED, allow_optional_failure=True)
        self.assertTrue(verdict.mergeable)
        self.assertTrue(any(message.startswith("WARNING") for message in verdict.messages))

    def test_case4_required_failure_blocks(self):
        entries = [RollupEntry("build", True, "FAILURE"),
                   RollupEntry("closing-references", True, "SUCCESS"),
                   RollupEntry("release-freeze", True, "SUCCESS")]
        self.assertFalse(judge(entries, REQUIRED).mergeable)

    def test_required_neutral_blocks(self):
        entries = [RollupEntry("build", True, "NEUTRAL"),
                   RollupEntry("closing-references", True, "SUCCESS"),
                   RollupEntry("release-freeze", True, "SUCCESS")]
        self.assertFalse(judge(entries, REQUIRED).mergeable)

    def test_case5_required_missing_from_rollup_blocks(self):
        entries = [RollupEntry("build", True, "SUCCESS"),
                   RollupEntry("closing-references", True, "SUCCESS")]
        verdict = judge(entries, REQUIRED)
        self.assertFalse(verdict.mergeable)
        self.assertIn("required but not reported: release-freeze", verdict.messages)

    def test_non_required_stale_blocks(self):
        entries = green_required() + [RollupEntry("GitGuardian Security Checks", True, "STALE")]
        self.assertFalse(judge(entries, REQUIRED).mergeable)

    def test_non_required_status_error_blocks(self):
        entries = green_required() + [RollupEntry("codecov/patch", True, "ERROR")]
        self.assertFalse(judge(entries, REQUIRED).mergeable)

    def test_all_green_merges(self):
        entries = green_required() + [RollupEntry("GitGuardian Security Checks", True, "SUCCESS")]
        self.assertTrue(judge(entries, REQUIRED).mergeable)

    def test_empty_rollup_blocks(self):
        self.assertFalse(judge([], REQUIRED).mergeable)

    def test_duplicate_required_runs_all_green_merge(self):
        # set-statuses style duplicates: two runs of one context, both green
        entries = green_required() + [RollupEntry("build", True, "SUCCESS")]
        self.assertTrue(judge(entries, REQUIRED).mergeable)

    def test_duplicate_required_runs_one_red_blocks(self):
        # the rollup does not date runs, so a red duplicate blocks (conservative)
        entries = green_required() + [RollupEntry("build", True, "FAILURE")]
        self.assertFalse(judge(entries, REQUIRED).mergeable)

    def test_required_reported_by_half_its_runs_counts_as_unreported(self):
        entries = green_required() + [RollupEntry("build", False, "PENDING")]
        verdict = judge(entries, REQUIRED)
        self.assertFalse(verdict.mergeable)
        self.assertIn("required but not reported: build", verdict.messages)

    def test_non_required_still_running_blocks(self):
        entries = green_required() + [RollupEntry("Socket Security: Project Report", False, "PENDING")]
        verdict = judge(entries, REQUIRED)
        self.assertFalse(verdict.mergeable)
        self.assertTrue(any("still running" in message for message in verdict.messages))


class ParseRollupTest(unittest.TestCase):
    """Check runs carry name/status/conclusion, commit statuses context/state."""

    def test_check_run_completed(self):
        entries = parse_rollup([{"name": "build", "status": "COMPLETED", "conclusion": "SUCCESS"}])
        self.assertEqual(entries, [RollupEntry("build", True, "SUCCESS")])

    def test_check_run_in_progress(self):
        entries = parse_rollup([{"name": "build", "status": "IN_PROGRESS", "conclusion": None}])
        self.assertEqual(entries, [RollupEntry("build", False, "PENDING")])

    def test_commit_status_success(self):
        entries = parse_rollup([{"context": "codecov/patch", "state": "SUCCESS"}])
        self.assertEqual(entries, [RollupEntry("codecov/patch", True, "SUCCESS")])

    def test_commit_status_pending_and_expected_not_reported(self):
        for state in ("PENDING", "EXPECTED"):
            entries = parse_rollup([{"context": "codecov/patch", "state": state}])
            self.assertEqual(entries, [RollupEntry("codecov/patch", False, state)])

    def test_nameless_entries_are_dropped(self):
        self.assertEqual(parse_rollup([{"status": "COMPLETED"}]), [])


class WaitTest(unittest.TestCase):
    """The wait ends only when everything on the head reported AND every
    required context exists in the rollup at all (#493, codecov posts late)."""

    def test_missing_required_context_keeps_waiting(self):
        entries = [RollupEntry("build", True, "SUCCESS")]
        self.assertFalse(all_reported(entries, REQUIRED))

    def test_unreported_entry_keeps_waiting(self):
        entries = green_required() + [RollupEntry("Socket Security: Project Report", False, "PENDING")]
        self.assertFalse(all_reported(entries, REQUIRED))

    def test_everything_reported_ends_the_wait(self):
        entries = green_required() + [RollupEntry("Socket Security: Project Report", True, "NEUTRAL")]
        self.assertTrue(all_reported(entries, REQUIRED))


class GuardTest(unittest.TestCase):
    """Every guard the Bash script carried (#324, #340, #395, #496)."""

    def test_base_target_mismatch_is_named(self):
        message = base_mismatch("develop", "hotfix/8.5.1")
        self.assertIn("develop", message)
        self.assertIn("hotfix/8.5.1", message)

    def test_base_target_agreement_passes(self):
        self.assertIsNone(base_mismatch("develop", "develop"))

    def test_dirty_tree_gets_a_note(self):
        self.assertIn("uncommitted changes", dirty_tree_note(" M file.txt\n"))

    def test_clean_tree_gets_no_note(self):
        self.assertIsNone(dirty_tree_note(""))

    def test_merge_pins_the_judged_head_and_never_deletes_the_branch(self):
        command = merge_command("876", "a" * 40)
        self.assertIn("--match-head-commit", command)
        self.assertIn("a" * 40, command)
        self.assertNotIn("--delete-branch", command)
        self.assertIn("--merge", command)

    def test_hotfix_without_protection_borrows_develops_checks(self):
        def fetch(branch):
            return REQUIRED if branch == "develop" else None
        contexts, note = resolve_required("hotfix/8.5.1", fetch)
        self.assertEqual(contexts, REQUIRED)
        self.assertIn("develop", note)

    def test_target_without_required_checks_resolves_empty(self):
        contexts, note = resolve_required("develop", lambda branch: None)
        self.assertEqual(contexts, [])
        self.assertIsNone(note)

    def test_non_hotfix_target_never_borrows(self):
        def fetch(branch):
            return REQUIRED if branch == "develop" else None
        contexts, _ = resolve_required("feature/x", fetch)
        self.assertEqual(contexts, [])


if __name__ == "__main__":
    unittest.main(verbosity=2)
