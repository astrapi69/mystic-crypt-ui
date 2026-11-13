#!/usr/bin/env python3
"""Fixture tests for merge_pr.py, no GitHub involved (#867).

Run: python3 scripts/merge_pr_test.py

The acceptance cases of the issue (decided by the maintainer on 2026-11-11)
plus every guard the Bash script carried, encoded as a test. Red before
green: this suite expresses the PORT's required behaviour; against the Bash
script the NEUTRAL and SKIPPED cases fail (the measured defect of #863), the
run stands in the pull request.
"""

import tempfile
import unittest
from pathlib import Path

from merge_pr import (RollupEntry, all_reported, base_mismatch, conventional_title_error,
                      feat_freeze_error, fragment_error, is_cross_repository,
                      dirty_tree_note, judge, merge_command, parse_checks, pr_state,
                      resolve_required, squash_subject)

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


class ParseChecksTest(unittest.TestCase):
    """The REST shapes a cloud session can read (#872): check runs from
    /commits/{sha}/check-runs, commit statuses from /commits/{sha}/status.
    GraphQL (gh pr view) answers 403 there."""

    def test_check_run_completed(self):
        entries = parse_checks([{"name": "build", "status": "completed",
                                 "conclusion": "success"}], [])
        self.assertEqual([RollupEntry("build", True, "SUCCESS")], entries)

    def test_check_run_in_progress(self):
        entries = parse_checks([{"name": "build", "status": "in_progress",
                                 "conclusion": None}], [])
        self.assertEqual([RollupEntry("build", False, "PENDING")], entries)

    def test_check_run_neutral_and_skipped_keep_their_verdict(self):
        entries = parse_checks([
            {"name": "a", "status": "completed", "conclusion": "neutral"},
            {"name": "b", "status": "completed", "conclusion": "skipped"}], [])
        self.assertEqual([RollupEntry("a", True, "NEUTRAL"),
                          RollupEntry("b", True, "SKIPPED")], entries)

    def test_commit_status_success(self):
        entries = parse_checks([], [{"context": "codecov/patch", "state": "success"}])
        self.assertEqual([RollupEntry("codecov/patch", True, "SUCCESS")], entries)

    def test_commit_status_pending_not_reported(self):
        entries = parse_checks([], [{"context": "codecov/patch", "state": "pending"}])
        self.assertEqual([RollupEntry("codecov/patch", False, "PENDING")], entries)

    def test_commit_status_error_blocks_like_failure(self):
        entries = parse_checks([], [{"context": "x", "state": "error"}])
        self.assertEqual([RollupEntry("x", True, "ERROR")], entries)

    def test_nameless_entries_are_dropped(self):
        self.assertEqual(parse_checks([{"status": "completed"}], [{"state": "success"}]), [])

    def test_a_required_check_run_again_green_on_the_same_head(self):
        # #969 at fd474774, as GitHub listed it: closing-references failed, the text was
        # corrected, the check ran again on "edited" and passed; the ids are GitHub's (#970)
        runs = [
            {"id": 131900732807, "name": "closing-references", "status": "completed",
             "conclusion": "success"},
            {"id": 131895963946, "name": "closing-references", "status": "completed",
             "conclusion": "failure"},
            {"id": 131895963001, "name": "build", "status": "completed", "conclusion": "success"}]
        statuses = [{"context": "release-freeze", "state": "success"}]
        entries = parse_checks(runs, statuses)
        self.assertEqual(2, [entry.name for entry in entries].count("closing-references"))
        self.assertFalse(judge(entries, REQUIRED).mergeable)


class PrStateTest(unittest.TestCase):
    """REST says state=open|closed plus a merged flag; the script's refusals
    name OPEN, MERGED or CLOSED as gh did (#872)."""

    def test_open(self):
        self.assertEqual("OPEN", pr_state({"state": "open", "merged": False}))

    def test_merged_wins_over_closed(self):
        self.assertEqual("MERGED", pr_state({"state": "closed", "merged": True}))

    def test_closed(self):
        self.assertEqual("CLOSED", pr_state({"state": "closed", "merged": False}))


class CrossRepositoryTest(unittest.TestCase):
    """The head branch is deleted only in the base repository; a fork's, or a
    deleted fork's, is left alone (#872)."""

    def test_same_repository(self):
        data = {"base": {"repo": {"full_name": "a/r"}}, "head": {"repo": {"full_name": "a/r"}}}
        self.assertFalse(is_cross_repository(data))

    def test_fork(self):
        data = {"base": {"repo": {"full_name": "a/r"}}, "head": {"repo": {"full_name": "b/r"}}}
        self.assertTrue(is_cross_repository(data))

    def test_deleted_fork_counts_as_cross(self):
        data = {"base": {"repo": {"full_name": "a/r"}}, "head": {"repo": None}}
        self.assertTrue(is_cross_repository(data))


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


class FeatFreezeTest(unittest.TestCase):
    """While feat-freeze.md lies in the repository root, a feat pull request
    is refused (the maintainer's bugfix-only decision of 2026-11-13, until
    the RELEASE-8.15 tag; the file goes with the tag)."""

    def freeze_file(self, directory, present=True):
        path = Path(directory) / "feat-freeze.md"
        if present:
            path.write_text("bugfix-only until RELEASE-8.15\n", encoding="utf-8")
        return path

    def test_a_feat_title_is_refused_while_the_file_lies_there(self):
        with tempfile.TemporaryDirectory() as directory:
            for title in ("feat: a thing", "feat(lethenon): a thing", "feat!: a break",
                          "feat(scope)!: a break"):
                message = feat_freeze_error(title, self.freeze_file(directory))
                self.assertIsNotNone(message, title)
                self.assertIn("feat-freeze.md", message)

    def test_every_other_type_passes_while_the_file_lies_there(self):
        with tempfile.TemporaryDirectory() as directory:
            freeze = self.freeze_file(directory)
            for title in ("fix: a", "test: b", "docs: c", "build: d", "chore: e",
                          "refactor: f", "perf: g", "ci: h"):
                self.assertIsNone(feat_freeze_error(title, freeze), title)

    def test_a_feat_title_passes_once_the_file_is_gone(self):
        with tempfile.TemporaryDirectory() as directory:
            absent = self.freeze_file(directory, present=False)
            self.assertIsNone(feat_freeze_error("feat: a thing", absent))

    def test_a_feature_scope_in_another_type_is_not_a_feat(self):
        with tempfile.TemporaryDirectory() as directory:
            freeze = self.freeze_file(directory)
            self.assertIsNone(feat_freeze_error("fix(feature-flags): a", freeze))


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

    def test_merge_squashes_with_the_title_as_subject(self):
        command = merge_command("876", "a" * 40, "feat(x): a thing (#876)", "why")
        self.assertIn("-X", command)
        self.assertIn("PUT", command)
        self.assertIn("repos/{owner}/{repo}/pulls/876/merge", command)
        self.assertIn("merge_method=squash", command)
        self.assertIn("commit_title=feat(x): a thing (#876)", command)
        self.assertIn("commit_message=why", command)

    def test_merge_pins_the_judged_head_and_never_deletes_the_branch(self):
        command = merge_command("876", "a" * 40, "feat: x (#876)", "")
        self.assertIn("sha=" + "a" * 40, command)
        self.assertNotIn("--delete-branch", command)
        self.assertEqual("gh", command[0])
        self.assertEqual("api", command[1])

    def test_squash_subject_appends_the_number_once(self):
        self.assertEqual("feat: x (#9)", squash_subject("feat: x", "9"))
        self.assertEqual("feat: x (#9)", squash_subject("feat: x (#9)", "9"))

    def test_feat_fix_perf_and_breaking_need_a_fragment(self):
        for title in ("feat: a", "fix(vault): b", "perf: c", "feat!: d", "refactor!: e"):
            message = fragment_error(title, "899", ["src/main/java/X.java"])
            self.assertIsNotNone(message, title)
            self.assertIn("changelog.d/899.", message)

    def test_a_present_fragment_satisfies_the_requirement(self):
        self.assertIsNone(fragment_error("feat: a", "899",
                                         ["changelog.d/899.added.md", "src/X.java"]))
        self.assertIsNone(fragment_error("fix: b", "899", ["changelog.d/899.fixed.md"]))
        self.assertIsNone(fragment_error("fix: b", "899", ["changelog.d/899.security.md"]))

    def test_mechanical_types_need_no_fragment(self):
        for title in ("chore: a", "test: b", "refactor: c", "ci: d", "build: e", "docs: f"):
            self.assertIsNone(fragment_error(title, "899", []), title)

    def test_another_pulls_fragment_does_not_count(self):
        self.assertIsNotNone(fragment_error("feat: a", "899", ["changelog.d/898.added.md"]))

    def test_a_wrong_section_does_not_count(self):
        self.assertIsNotNone(fragment_error("feat: a", "899", ["changelog.d/899.misc.md"]))

    def test_conventional_titles_pass(self):
        for title in ("feat: a", "fix(vault): b", "feat!: breaking", "chore(deps): c",
                      "docs: d", "refactor(x): e"):
            self.assertIsNone(conventional_title_error(title), title)

    def test_a_title_with_an_issue_number_is_refused(self):
        # the number belongs in the text as Closes #<n>; the merge appends the PR number itself
        message = conventional_title_error("fix(vault): a save keeps its backup (#847)")
        self.assertIsNotNone(message)
        self.assertIn("Closes", message)

    def test_non_conventional_titles_are_refused(self):
        for title in ("Update README", "Feat: capital", "feat:missing space", "", None,
                      "feature: wrong type"):
            self.assertIsNotNone(conventional_title_error(title), str(title))

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
