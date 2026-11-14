#!/usr/bin/env python3
"""Fixture tests for merge_pr.py, no GitHub involved (#867).

Run: python3 scripts/merge_pr_test.py

The acceptance cases of the issue (decided by the maintainer on 2026-11-11)
plus every guard the Bash script carried, encoded as a test. Red before
green: this suite expresses the PORT's required behaviour; against the Bash
script the NEUTRAL and SKIPPED cases fail (the measured defect of #863), the
run stands in the pull request.
"""

import contextlib
import io
import json
import subprocess
import tempfile
import unittest
from pathlib import Path

from merge_pr import (RollupEntry, all_reported, base_mismatch, branch_is_gone,
                      conventional_title_error,
                      feat_freeze_error, fragment_error, is_cross_repository,
                      dirty_tree_note, judge, merge_command, parse_checks, pr_state,
                      resolve_required, squash_subject, wait_for_checks)

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
        self.assertEqual([RollupEntry("closing-references", True, "SUCCESS")],
                         [entry for entry in entries if entry.name == "closing-references"])
        self.assertTrue(judge(entries, REQUIRED).mergeable)

    def test_the_newest_run_of_a_check_decides(self):
        # GitHub numbers check runs in the order it creates them, so the highest id is the
        # newest, the one branch protection judges by (#970)
        cases = [
            ("a newer green run replaces a red one", ("failure", "success"),
             [RollupEntry("build", True, "SUCCESS")]),
            ("a newer red run replaces a green one", ("success", "failure"),
             [RollupEntry("build", True, "FAILURE")]),
            ("a newer run still in progress is not reported yet", ("success", None),
             [RollupEntry("build", False, "PENDING")]),
        ]
        for label, (older, newer), expected in cases:
            with self.subTest(label):
                runs = [
                    {"id": 20, "name": "build",
                     "status": "completed" if newer else "in_progress", "conclusion": newer},
                    {"id": 10, "name": "build", "status": "completed", "conclusion": older}]
                self.assertEqual(expected, parse_checks(runs, []))

    def test_a_newer_run_of_another_app_does_not_replace_the_real_one(self):
        # The name alone is not the identity of a check (#976): a newer green run posted by
        # ANOTHER app under a required check's name stays BESIDE the real red run instead of
        # replacing it, and the red one blocks through judge's duplicate rule.
        runs = [
            {"id": 30, "name": "build", "status": "completed", "conclusion": "success",
             "app": "some-other-app"},
            {"id": 20, "name": "build", "status": "completed", "conclusion": "failure",
             "app": "github-actions"}]
        entries = parse_checks(runs, [])
        self.assertEqual([RollupEntry("build", True, "SUCCESS"),
                          RollupEntry("build", True, "FAILURE")], entries)
        self.assertFalse(judge(entries + [RollupEntry("closing-references", True, "SUCCESS"),
                                          RollupEntry("release-freeze", True, "SUCCESS")],
                               REQUIRED).mergeable,
                         "the foreign green must not hide the red required run")

    def test_a_rerun_of_the_same_apps_check_still_dedupes(self):
        # within one app the newest run of a name still replaces the older one (#970)
        runs = [
            {"id": 30, "name": "build", "status": "completed", "conclusion": "success",
             "app": "github-actions"},
            {"id": 20, "name": "build", "status": "completed", "conclusion": "failure",
             "app": "github-actions"}]
        self.assertEqual([RollupEntry("build", True, "SUCCESS")], parse_checks(runs, []))

    def test_runs_of_different_checks_are_all_kept(self):
        runs = [{"id": 2, "name": "build", "status": "completed", "conclusion": "success"},
                {"id": 1, "name": "CodeQL", "status": "completed", "conclusion": "failure"}]
        self.assertEqual([RollupEntry("build", True, "SUCCESS"),
                          RollupEntry("CodeQL", True, "FAILURE")], parse_checks(runs, []))

    def test_a_run_without_an_id_cannot_be_dated_and_is_kept(self):
        # nothing tells which of two undated runs is newer, so a red one still blocks
        runs = [{"name": "build", "status": "completed", "conclusion": "success"},
                {"name": "build", "status": "completed", "conclusion": "failure"}]
        entries = parse_checks(runs, [])
        self.assertEqual([RollupEntry("build", True, "SUCCESS"),
                          RollupEntry("build", True, "FAILURE")], entries)
        self.assertFalse(judge(entries + [RollupEntry("closing-references", True, "SUCCESS"),
                                          RollupEntry("release-freeze", True, "SUCCESS")],
                               REQUIRED).mergeable)


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


class WaitForChecksTest(unittest.TestCase):
    """The wait loop of run(), driven with a poll that stands in for the two
    GitHub calls (#971)."""

    @staticmethod
    def polls(*answers):
        """A poll that gives the answers in order: entries, or an exception to raise"""
        queue = list(answers)

        def poll():
            answer = queue.pop(0)
            if isinstance(answer, Exception):
                raise answer
            return answer
        return poll

    def test_the_wait_ends_once_everything_reported(self):
        sleeps = []
        running = green_required() + [RollupEntry("CodeQL", False, "PENDING")]
        entries, refusal = wait_for_checks(self.polls(running, green_required()), REQUIRED, 40,
                                           poll_seconds=20, sleep=sleeps.append)
        self.assertEqual(green_required(), entries)
        self.assertIsNone(refusal)
        self.assertEqual([20], sleeps)

    def test_a_wait_that_reaches_its_limit_refuses_and_names_what_runs(self):
        running = green_required() + [RollupEntry("CodeQL", False, "PENDING")]
        entries, refusal = wait_for_checks(self.polls(running, running, running), REQUIRED, 40,
                                           poll_seconds=20, sleep=lambda seconds: None)
        self.assertIsNone(entries)
        self.assertEqual("after 40s - still running: CodeQL; required but not reported: none"
                         " - not merging", refusal)

    def test_a_failed_look_is_no_answer_and_the_wait_looks_again(self):
        # #969 on 2026-11-13: gh api answered HTTP 502 once while the checks still ran (#971)
        cases = [
            ("a GitHub call that ends with an error",
             subprocess.CalledProcessError(1, ["gh", "api"], stderr="gh: HTTP 502\n"),
             "gh: HTTP 502"),
            ("a GitHub call that ends without a word on standard error",
             subprocess.CalledProcessError(1, ["gh", "api"], stderr=""), "exit status 1"),
            ("an answer that is not JSON",
             json.JSONDecodeError("Expecting value", "bad gateway", 0),
             "the answer was not JSON: Expecting value"),
        ]
        for label, failure, named in cases:
            with self.subTest(label):
                sleeps = []
                printed = io.StringIO()
                with contextlib.redirect_stdout(printed):
                    entries, refusal = wait_for_checks(
                        self.polls(failure, green_required()), REQUIRED, 40, poll_seconds=20,
                        sleep=sleeps.append)
                self.assertEqual(green_required(), entries)
                self.assertIsNone(refusal)
                self.assertEqual([20], sleeps)
                self.assertEqual(f"the look at the checks failed ({named}); looking again in 20s\n",
                                 printed.getvalue())

    def test_looks_that_fail_until_the_limit_refuse_and_name_the_failure(self):
        failed = subprocess.CalledProcessError(1, ["gh", "api"], stderr="gh: HTTP 502\n")
        running = green_required() + [RollupEntry("CodeQL", False, "PENDING")]
        with contextlib.redirect_stdout(io.StringIO()):
            entries, refusal = wait_for_checks(self.polls(running, failed, failed), REQUIRED, 40,
                                               poll_seconds=20, sleep=lambda seconds: None)
        self.assertIsNone(entries)
        self.assertEqual("after 40s - the last look at the checks failed (gh: HTTP 502)"
                         " - not merging", refusal)


class BranchIsGoneTest(unittest.TestCase):
    """After the merge the script says whether the head branch is gone (#974)."""

    @staticmethod
    def answers(*still_there):
        queue = list(still_there)
        return lambda: queue.pop(0)

    def test_a_branch_gone_at_the_first_answer_is_gone_without_a_wait(self):
        pauses = []
        self.assertTrue(branch_is_gone(self.answers(False), sleep=pauses.append))
        self.assertEqual([], pauses)

    def test_a_branch_still_there_right_after_the_deletion_is_asked_about_again(self):
        # four merges on 2026-11-13: the first answer said "still there", the branch was gone (#974)
        pauses = []
        self.assertTrue(branch_is_gone(self.answers(True, True, False), sleep=pauses.append))
        self.assertEqual([3, 3], pauses)

    def test_a_branch_still_there_at_every_answer_gets_the_note(self):
        pauses = []
        self.assertFalse(branch_is_gone(self.answers(True, True, True, True, True),
                                        sleep=pauses.append))
        self.assertEqual([3, 3, 3, 3], pauses)


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
