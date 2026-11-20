#!/usr/bin/env python3
"""Tests for pr_state_guard.py, the pre-push check of AG-061, with GitHub replaced by a fake.

Run: python3 scripts/pr_state_guard_test.py
"""

import io
import unittest

from pr_state_guard import (GhError, branches_pushed, main, repository_of,
                            verdict)

ZERO = "0" * 40
SHA = "1" * 40


def push_line(branch, local_sha=SHA):
    return f"refs/heads/{branch} {local_sha} refs/heads/{branch} {SHA}\n"


def merged(number):
    return {"number": number, "state": "closed", "merged_at": "2026-11-20T09:02:51Z"}


def closed(number):
    return {"number": number, "state": "closed", "merged_at": None}


def opened(number):
    return {"number": number, "state": "open", "merged_at": None}


class FakeGitHub:
    """Answers the pull requests of a branch from a table, and records what was asked"""

    def __init__(self, pulls_by_branch=None, failure=None):
        self.pulls_by_branch = pulls_by_branch or {}
        self.failure = failure
        self.asked = []

    def __call__(self, repository, owner, branch):
        self.asked.append((repository, owner, branch))
        if self.failure:
            raise GhError(self.failure)
        return self.pulls_by_branch.get(branch, [])


def run(stdin_text, github, url="git@github.com:astrapi69/mystic-crypt-ui.git"):
    out = io.StringIO()
    code = main(["pre-push", "origin", url], io.StringIO(stdin_text), out, github)
    return code, out.getvalue()


class VerdictTest(unittest.TestCase):

    def test_a_merged_pull_request_refuses_the_push(self):
        message = verdict("chore/x", [merged(1086)])
        self.assertIn("#1086", message)
        self.assertIn("merged", message)
        self.assertIn("AG-061", message)

    def test_a_closed_pull_request_refuses_the_push(self):
        message = verdict("chore/x", [closed(1061)])
        self.assertIn("#1061", message)
        self.assertIn("closed", message)

    def test_an_open_pull_request_lets_the_push_through(self):
        self.assertIsNone(verdict("chore/x", [opened(1088)]))

    def test_an_open_pull_request_beside_an_old_merged_one_lets_the_push_through(self):
        self.assertIsNone(verdict("chore/x", [merged(900), opened(1088)]))

    def test_a_branch_without_a_pull_request_lets_the_push_through(self):
        self.assertIsNone(verdict("chore/new", []))

    def test_the_newest_of_several_finished_pull_requests_is_named(self):
        message = verdict("chore/x", [merged(900), closed(1061)])
        self.assertIn("#1061", message)
        self.assertNotIn("#900", message)


class BranchesPushedTest(unittest.TestCase):

    def test_a_branch_push_is_found(self):
        self.assertEqual(["fix/a"], branches_pushed(io.StringIO(push_line("fix/a"))))

    def test_a_deletion_is_left_out(self):
        self.assertEqual([], branches_pushed(io.StringIO(push_line("fix/a", ZERO))))

    def test_a_tag_is_left_out(self):
        line = f"refs/tags/RELEASE-8.16 {SHA} refs/tags/RELEASE-8.16 {ZERO}\n"
        self.assertEqual([], branches_pushed(io.StringIO(line)))

    def test_the_long_lived_branches_are_left_out(self):
        lines = push_line("develop") + push_line("master") + push_line("main")
        self.assertEqual([], branches_pushed(io.StringIO(lines)))

    def test_a_push_to_another_remote_branch_name_names_the_remote_one(self):
        line = f"HEAD {SHA} refs/heads/chore/remote-name {SHA}\n"
        self.assertEqual(["chore/remote-name"], branches_pushed(io.StringIO(line)))


class RepositoryOfTest(unittest.TestCase):

    def test_the_forms_of_a_github_url(self):
        for url in ("git@github.com:astrapi69/mystic-crypt-ui.git",
                    "https://github.com/astrapi69/mystic-crypt-ui.git",
                    "https://github.com/astrapi69/mystic-crypt-ui",
                    "ssh://git@github.com/astrapi69/mystic-crypt-ui.git"):
            self.assertEqual("astrapi69/mystic-crypt-ui", repository_of(url), url)

    def test_a_remote_that_is_not_github(self):
        self.assertIsNone(repository_of("/home/user/backup.git"))


class MainTest(unittest.TestCase):

    def test_a_push_to_the_branch_of_a_merged_pull_request_is_refused(self):
        github = FakeGitHub({"chore/ci-pin-upload-artifact": [merged(1086)]})
        code, out = run(push_line("chore/ci-pin-upload-artifact"), github)
        self.assertEqual(1, code)
        self.assertIn("#1086", out)
        self.assertEqual([("astrapi69/mystic-crypt-ui", "astrapi69", "chore/ci-pin-upload-artifact")],
                         github.asked)

    def test_a_push_to_the_branch_of_an_open_pull_request_goes_through(self):
        code, out = run(push_line("chore/x"), FakeGitHub({"chore/x": [opened(1088)]}))
        self.assertEqual(0, code)
        self.assertEqual("", out)

    def test_github_that_cannot_answer_refuses_the_push(self):
        code, out = run(push_line("chore/x"), FakeGitHub(failure="HTTP 502"))
        self.assertEqual(1, code, "a check that cannot check does not report green")
        self.assertIn("HTTP 502", out)

    def test_a_push_of_develop_asks_nothing(self):
        github = FakeGitHub(failure="must not be asked")
        code, _ = run(push_line("develop"), github)
        self.assertEqual(0, code)
        self.assertEqual([], github.asked)

    def test_a_remote_that_is_not_github_asks_nothing(self):
        github = FakeGitHub(failure="must not be asked")
        code, _ = run(push_line("chore/x"), github, url="/home/user/backup.git")
        self.assertEqual(0, code)
        self.assertEqual([], github.asked)

    def test_one_refused_branch_among_several_refuses_the_whole_push(self):
        github = FakeGitHub({"chore/a": [opened(1)], "chore/b": [merged(2)]})
        code, out = run(push_line("chore/a") + push_line("chore/b"), github)
        self.assertEqual(1, code)
        self.assertIn("chore/b", out)
        self.assertNotIn("chore/a ", out)


if __name__ == "__main__":
    unittest.main()
