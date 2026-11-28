#!/usr/bin/env python3
"""Tests for private_references.py, the check for references to the private notes (#1103).

Every test makes up its own pattern list in a temporary directory and never reads the maintainer's: a
made-up repository name, a made-up home path and a made-up rule id. The machine's git configuration
is shut out (GIT_CONFIG_GLOBAL, GIT_CONFIG_NOSYSTEM, HOME), so a setting there cannot reach a test.

The check is driven the way its callers drive it: as a program, with git's pre-push lines or Claude
Code's hook input on standard input; the pre-push hook through a real git push to a bare repository
beside the test's own; the tool hook through the command .claude/settings.json gives it.

Run: python3 scripts/private_references_test.py
"""

import json
import os
import re
import shutil
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SCRIPT = ROOT / "scripts" / "private_references.py"
SETTINGS = ROOT / ".claude" / "settings.json"
VARIABLE = "MYSTIC_CRYPT_PRIVATE_REFERENCES"
GIT_KEY = "mysticcrypt.privateReferences"
ZERO = "0" * 40

# made up for the tests: none of it names anything real
PATTERNS = "\n".join([
    "# a made-up list",
    "private-scratchbook",
    "/home/someone/",
    r"\bQX-9\d\d\b",
    "",
])
REPOSITORY_LINE, HOME_LINE, RULE_LINE = 2, 3, 4


def push_line(branch, local_sha, remote_sha):
    return f"refs/heads/{branch} {local_sha} refs/heads/{branch} {remote_sha}\n"


def matcher_matches(matcher, tool_name):
    """Claude Code's rule for a hook matcher (hooks reference, "Matcher patterns"): "*", "" or none
    matches every tool; one of only letters, digits, "_", "-", spaces, "," and "|" is a list of exact
    names; anything else is a JavaScript regular expression, tested unanchored"""
    if matcher in ("", "*"):
        return True
    if re.fullmatch(r"[A-Za-z0-9_\- ,|]*", matcher):
        return tool_name in {name.strip() for name in re.split(r"[|,]", matcher)}
    return re.search(matcher, tool_name) is not None


class Sandbox(unittest.TestCase):
    """A temporary directory, a made-up pattern list in it, and an environment the machine cannot reach"""

    def setUp(self):
        self.directory = Path(tempfile.mkdtemp(prefix="private-references-test-"))
        self.addCleanup(shutil.rmtree, self.directory, ignore_errors=True)
        self.patterns = self.write("patterns.txt", PATTERNS)
        self.global_config = self.write("gitconfig", "")
        # CLAUDE_PROJECT_DIR too: the tool hook reads the git config of that checkout
        self.environment = {key: value for key, value in os.environ.items()
                            if key not in (VARIABLE, "CLAUDE_PROJECT_DIR") and not key.startswith("GIT_")}
        self.environment.update({
            "HOME": str(self.directory),
            "GIT_CONFIG_GLOBAL": str(self.global_config),
            "GIT_CONFIG_NOSYSTEM": "1",
            "GIT_AUTHOR_NAME": "Test Person",
            "GIT_AUTHOR_EMAIL": "test@example.invalid",
            "GIT_COMMITTER_NAME": "Test Person",
            "GIT_COMMITTER_EMAIL": "test@example.invalid",
        })

    def write(self, name, text):
        path = self.directory / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text, encoding="utf-8")
        return path

    def named(self, path=None):
        """The environment with the variable naming a list, this sandbox's own when none is given"""
        environment = dict(self.environment)
        environment[VARIABLE] = str(path or self.patterns)
        return environment

    def check(self, arguments, stdin="", environment=None, cwd=None):
        return subprocess.run([sys.executable, "-I", str(SCRIPT), *arguments], input=stdin,
                              capture_output=True, text=True,
                              env=self.named() if environment is None else environment,
                              cwd=str(cwd or self.directory))


class Repository:
    """A throwaway git repository in a sandbox, with a bare repository beside it as its remote"""

    def __init__(self, sandbox):
        self.sandbox = sandbox
        self.work = sandbox.directory / "work"
        self.remote = sandbox.directory / "remote.git"
        self.git_in(sandbox.directory, "init", "-q", "--bare", str(self.remote))
        self.git_in(sandbox.directory, "init", "-q", "-b", "main", str(self.work))
        self.git("remote", "add", "origin", str(self.remote))

    def git_in(self, cwd, *arguments):
        result = subprocess.run(["git", *arguments], cwd=str(cwd), capture_output=True, text=True,
                                env=self.sandbox.environment)
        if result.returncode:
            raise AssertionError(f"git {' '.join(arguments)} failed: {result.stderr}")
        return result.stdout.strip()

    def git(self, *arguments):
        return self.git_in(self.work, *arguments)

    def commit(self, files, message="a change"):
        """Writes the files (None deletes one), commits them and returns the commit"""
        for name, text in files.items():
            path = self.work / name
            if text is None:
                path.unlink()
            else:
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_text(text, encoding="utf-8")
        self.git("add", "-A", "--", *files)
        self.git("commit", "-q", "-m", message)
        return self.git("rev-parse", "HEAD")

    def publish(self, branch="main"):
        """Pushes HEAD without a hook and fetches, so its commits are on the remote and known to be"""
        self.git("push", "-q", "origin", f"HEAD:refs/heads/{branch}")
        self.git("fetch", "-q", "origin")

    def on_remote(self, branch):
        return subprocess.run(["git", "--git-dir", str(self.remote), "rev-parse", "--verify", "-q",
                               f"refs/heads/{branch}"], capture_output=True,
                              env=self.sandbox.environment).returncode == 0


class SettingTest(Sandbox):
    """Where the list comes from, and what happens when it cannot be had"""

    def setUp(self):
        super().setUp()
        self.repository = Repository(self)
        base = self.repository.commit({"README.md": "a project\n"})
        self.repository.publish()
        head = self.repository.commit({"notes.md": "see private-scratchbook/coding/x.md\n"})
        self.lines = push_line("main", head, base)

    def push_check(self, environment):
        return self.check(["--push", "origin", str(self.repository.remote)], self.lines, environment,
                          self.repository.work)

    def test_without_a_setting_the_push_goes_through_and_says_that_it_did_not_check(self):
        result = self.push_check(self.environment)
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn("not checked", result.stderr)
        self.assertNotIn("none found", result.stderr)

    def test_a_list_that_cannot_be_read_refuses_the_push_without_printing_its_path(self):
        missing = self.directory / "missing.txt"
        result = self.push_check(self.named(missing))
        self.assertEqual(1, result.returncode, "a check that cannot check does not report green")
        self.assertIn("cannot be read", result.stderr)
        self.assertIn(VARIABLE, result.stderr)
        self.assertNotIn(str(missing), result.stderr, "the message may land in a public text")

    def test_a_list_without_a_pattern_refuses_the_push(self):
        empty = self.write("empty.txt", "# only a comment\n\n   \n")
        result = self.push_check(self.named(empty))
        self.assertEqual(1, result.returncode)
        self.assertIn("no pattern", result.stderr)

    def test_a_pattern_that_is_no_regular_expression_refuses_the_push_and_names_its_line(self):
        broken = self.write("broken.txt", "# a comment\nprivate-(scratchbook\n")
        result = self.push_check(self.named(broken))
        self.assertEqual(1, result.returncode)
        self.assertIn("line 2", result.stderr)
        self.assertNotIn("private-(scratchbook", result.stderr)

    def test_the_git_config_key_names_the_list_when_the_variable_is_not_set(self):
        self.repository.git("config", "--file", str(self.global_config), GIT_KEY, str(self.patterns))
        result = self.push_check(self.environment)
        self.assertEqual(1, result.returncode, result.stderr)
        self.assertIn("notes.md line 1", result.stderr)

    def test_the_variable_wins_over_the_git_config_key(self):
        self.repository.git("config", "--file", str(self.global_config), GIT_KEY,
                            str(self.directory / "missing.txt"))
        clean = self.write("clean.txt", "nothing-here-matches\n")
        result = self.push_check(self.named(clean))
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn("none found", result.stderr)


class PushTest(Sandbox):
    """--push: the added lines and the message of every commit a push adds"""

    def setUp(self):
        super().setUp()
        self.repository = Repository(self)
        self.base = self.repository.commit({"README.md": "a project\n"})
        self.repository.publish()

    def push(self, lines):
        return self.check(["--push", "origin", str(self.repository.remote)], lines, cwd=self.repository.work)

    def test_an_added_line_that_names_the_private_notes_refuses_the_push_naming_commit_file_and_line(self):
        head = self.repository.commit({"docs/notes.md": "first line\nsee private-scratchbook/coding/x.md\n"})
        result = self.push(push_line("main", head, self.base))
        self.assertEqual(1, result.returncode, result.stderr)
        self.assertIn(f"commit {head[:12]} docs/notes.md line 2", result.stderr)
        self.assertIn(f"pattern on line {REPOSITORY_LINE} of the list", result.stderr)
        self.assertNotIn("private-scratchbook", result.stderr, "a finding names where, never what")

    def test_every_pattern_of_the_list_is_searched_in_every_added_line(self):
        head = self.repository.commit({"a.md": "fine\nunder /home/someone/dev\nas QX-917 says\n"})
        result = self.push(push_line("main", head, self.base))
        self.assertEqual(1, result.returncode, result.stderr)
        self.assertIn(f"a.md line 2: pattern on line {HOME_LINE} of the list", result.stderr)
        self.assertIn(f"a.md line 3: pattern on line {RULE_LINE} of the list", result.stderr)
        self.assertNotIn("a.md line 1", result.stderr)

    def test_a_removed_line_that_names_the_private_notes_is_no_finding(self):
        dirty = self.repository.commit({"notes.md": "keep\nprivate-scratchbook\n"})
        self.repository.publish()
        clean = self.repository.commit({"notes.md": "keep\n"})
        result = self.push(push_line("main", clean, dirty))
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn("0 added lines and 1 commit message checked against 3 patterns, none found", result.stderr)

    def test_a_commit_message_that_names_the_private_notes_refuses_the_push(self):
        head = self.repository.commit({"a.md": "fine\n"}, message="a subject\n\nas QX-917 asks")
        result = self.push(push_line("main", head, self.base))
        self.assertEqual(1, result.returncode, result.stderr)
        self.assertIn(f"commit {head[:12]} message line 3: pattern on line {RULE_LINE}", result.stderr)

    def test_a_new_branch_is_checked_from_where_it_leaves_what_the_remote_has(self):
        # a text the remote already carries is not the push's doing
        self.repository.commit({"old.md": "private-scratchbook\n"})
        self.repository.publish()
        head = self.repository.commit({"a.md": "fine\n"})
        result = self.push(push_line("feature/x", head, ZERO))
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn("1 added line and 1 commit message checked", result.stderr)

    def test_a_deleted_branch_has_nothing_to_check(self):
        result = self.push(f"(delete) {ZERO} refs/heads/feature/x {self.base}\n")
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn("nothing to check", result.stderr)

    def test_a_clean_push_says_what_it_checked(self):
        self.repository.commit({"a.md": "one\n"})
        head = self.repository.commit({"b.md": "two\nthree\n"})
        result = self.push(push_line("main", head, self.base))
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn("3 added lines and 2 commit messages checked against 3 patterns, none found",
                      result.stderr)

    def merge_from_a_published_branch(self):
        """feature/x, with a commit of its own, merging a branch the remote has, which names the notes"""
        self.repository.git("checkout", "-q", "-b", "other")
        self.repository.commit({"other.md": "private-scratchbook\n"})
        self.repository.publish("other")
        self.repository.git("checkout", "-q", "-b", "feature/x", "main")
        self.repository.commit({"a.md": "fine\n"})

    def test_a_merge_is_checked_for_what_it_adds_beyond_its_parents(self):
        self.merge_from_a_published_branch()
        self.repository.git("merge", "-q", "--no-edit", "origin/other")
        head = self.repository.git("rev-parse", "HEAD")
        result = self.push(push_line("feature/x", head, ZERO))
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn("1 added line and 2 commit messages checked", result.stderr)

    def test_a_line_a_merge_adds_beyond_its_parents_is_found(self):
        self.merge_from_a_published_branch()
        self.repository.git("merge", "-q", "--no-commit", "--no-ff", "origin/other")
        (self.repository.work / "README.md").write_text("a project\nsee /home/someone/x\n", encoding="utf-8")
        self.repository.git("add", "--", "README.md")
        self.repository.git("commit", "-q", "-m", "a merge")
        head = self.repository.git("rev-parse", "HEAD")
        result = self.push(push_line("feature/x", head, ZERO))
        self.assertEqual(1, result.returncode, result.stderr)
        self.assertIn(f"commit {head[:12]} README.md line 2: pattern on line {HOME_LINE}", result.stderr)
        self.assertNotIn("other.md", result.stderr, "what the remote has is not checked again")


class FileTest(Sandbox):
    """--file: a text before it is sent, for example a body file before gh"""

    def test_a_file_that_names_the_private_notes_is_a_finding(self):
        body = self.write("body.md", "A title\n\nsee private-scratchbook\n")
        result = self.check(["--file", str(body)])
        self.assertEqual(1, result.returncode, result.stderr)
        self.assertIn(f"{body} line 3: pattern on line {REPOSITORY_LINE}", result.stderr)

    def test_a_clean_file_says_how_much_it_checked(self):
        body = self.write("body.md", "A title\n\nnothing private\n")
        result = self.check(["--file", str(body)])
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn("1 file of 3 lines checked against 3 patterns, none found", result.stderr)

    def test_a_file_that_cannot_be_read_is_refused(self):
        result = self.check(["--file", str(self.directory / "missing.md")])
        self.assertEqual(1, result.returncode, result.stderr)
        self.assertIn("cannot be read", result.stderr)

    def test_without_a_setting_a_file_is_not_checked_and_says_so(self):
        body = self.write("body.md", "see private-scratchbook\n")
        result = self.check(["--file", str(body)], environment=self.environment)
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn("not checked", result.stderr)


class ToolCallTest(Sandbox):
    """--tool-call: what a gh call or a GitHub MCP tool call would publish, as a PreToolUse hook sees it"""

    def setUp(self):
        super().setUp()
        self.dirty = self.write("body.md", "Summary\n\nsee /home/someone/dev/x\n")
        self.clean = self.write("clean.md", "Summary\n\nnothing private\n")

    def call(self, tool_name, tool_input, environment=None):
        payload = {"session_id": "test", "cwd": str(self.directory), "hook_event_name": "PreToolUse",
                   "tool_name": tool_name, "tool_input": tool_input}
        return self.check(["--tool-call"], json.dumps(payload), environment)

    def bash(self, command, environment=None):
        return self.call("Bash", {"command": command, "description": "a test"}, environment)

    def assertBlocked(self, result, *expected):
        self.assertEqual(2, result.returncode, f"exit 2 blocks the call in Claude Code\n{result.stderr}")
        for text in expected:
            self.assertIn(text, result.stderr)

    def assertSilent(self, result):
        self.assertEqual((0, "", ""), (result.returncode, result.stdout, result.stderr))

    def test_a_body_file_of_gh_that_names_the_private_notes_blocks_the_call(self):
        result = self.bash(f"gh pr create --base develop --title 'a title' --body-file {self.dirty}")
        self.assertBlocked(result, f"{self.dirty} line 3: pattern on line {HOME_LINE}")
        self.assertNotIn("/home/someone", result.stderr.replace(str(self.dirty), ""))

    def test_every_way_gh_takes_a_text_is_checked(self):
        dirty = self.dirty
        self.write("sub/only-here.md", "Summary\n\nas QX-917 says\n")
        for command in (f"gh issue comment 5 -F {dirty}",
                        f"gh issue create --title t --body-file={dirty}",
                        f"gh api -X PATCH repos/o/r/pulls/7 -F body=@{dirty}",
                        f"gh api repos/o/r/issues --input {dirty}",
                        f"gh pr create --title 'as QX-917 says' --body-file {self.clean}",
                        "gh pr edit 7 --body 'see private-scratchbook'",
                        "gh pr edit 7 --body='see private-scratchbook'",
                        "gh pr merge 7 --squash --subject 'see private-scratchbook'",
                        f"gh release create RELEASE-1 --notes-file {dirty}",
                        "gh pr comment 3 -F body.md",
                        f"bash -c 'cd {self.directory}/sub && gh issue comment 5 --body-file only-here.md'",
                        f"set -e\ngh pr comment 3 -F {dirty}",
                        "env GH_PAGER= gh api graphql -f query='mutation { addComment(input: {body: \"QX-917\"}) }'"):
            with self.subTest(command=command):
                # a finding, not a file that could not be read: the text was found and checked
                self.assertBlocked(self.bash(command), "blocked", "pattern on line")

    def test_a_text_on_standard_input_is_checked_where_its_file_is_named_and_blocks_where_not(self):
        self.assertBlocked(self.bash("gh pr comment 3 --body-file -"), "standard input")
        self.assertBlocked(self.bash(f"gh pr comment 3 --body-file - < {self.dirty}"), "line 3")
        self.assertEqual(0, self.bash(f"gh pr comment 3 --body-file - < {self.clean}").returncode)

    def test_a_body_file_that_cannot_be_read_blocks_the_call(self):
        self.assertBlocked(self.bash("gh pr create --title t --body-file missing.md"), "cannot be read")

    def test_a_clean_gh_call_says_what_it_checked(self):
        result = self.bash(f"gh pr create --title 'a title' --body-file {self.clean}")
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn("2 texts of 4 lines checked against 3 patterns, none found", result.stdout)

    def test_calls_that_publish_nothing_are_not_checked(self):
        for command in ("gh pr view 7",
                        "gh run list --limit 5",
                        "gh workflow run gradle.yml --ref x -F force=true",
                        "gh api -X GET search/issues -f q=private-scratchbook",
                        "git commit -m 'a word: gh'",
                        "cat /home/someone/notes.md",
                        "echo 'it is unclosed"):
            with self.subTest(command=command):
                self.assertSilent(self.bash(command))

    def test_a_gh_call_that_cannot_be_split_into_words_blocks(self):
        self.assertBlocked(self.bash("gh pr comment 3 --body 'unclosed"), "cannot be split")

    def test_a_github_mcp_write_that_names_the_private_notes_is_blocked(self):
        result = self.call("mcp__github__create_pull_request",
                           {"owner": "o", "repo": "r", "title": "a title", "head": "x", "base": "develop",
                            "body": "a line\nsee private-scratchbook\n"})
        self.assertBlocked(result, f"body line 2: pattern on line {REPOSITORY_LINE}")

    def test_every_text_of_a_github_mcp_write_is_checked(self):
        for tool_name, tool_input, expected in (
                ("mcp__github__add_issue_comment",
                 {"owner": "o", "repo": "r", "issue_number": 5, "body": "QX-917"}, "body line 1"),
                ("mcp__github__issue_write",
                 {"method": "create", "owner": "o", "repo": "r", "title": "private-scratchbook"}, "title line 1"),
                ("mcp__github__merge_pull_request",
                 {"owner": "o", "repo": "r", "pullNumber": 7, "commit_message": "as QX-917 says"},
                 "commit_message line 1"),
                ("mcp__github__push_files",
                 {"owner": "o", "repo": "r", "branch": "b", "message": "m",
                  "files": [{"path": "a.md", "content": "x\n/home/someone/y\n"}]}, "files[0].content line 2")):
            with self.subTest(tool_name=tool_name):
                self.assertBlocked(self.call(tool_name, tool_input), expected)

    def test_a_github_mcp_read_is_not_checked(self):
        for tool_name, tool_input in (
                ("mcp__github__get_file_contents", {"owner": "o", "repo": "r", "path": "private-scratchbook.md"}),
                ("mcp__github__search_code", {"query": "private-scratchbook"}),
                ("mcp__github__issue_read", {"method": "get", "owner": "o", "repo": "r", "issue_number": 1})):
            with self.subTest(tool_name=tool_name):
                self.assertSilent(self.call(tool_name, tool_input))

    def test_a_write_into_the_repository_the_list_names_is_not_checked(self):
        result = self.call("mcp__github__create_or_update_file",
                           {"owner": "someone", "repo": "private-scratchbook", "path": "STATUS.md",
                            "content": "under /home/someone/x\n", "message": "m", "branch": "main"})
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn("not checked", result.stdout)
        result = self.bash(f"gh issue create -R someone/private-scratchbook --title t --body-file {self.dirty}")
        self.assertEqual(0, result.returncode, result.stderr)

    def test_other_tools_are_not_checked(self):
        self.assertSilent(self.call("Read", {"file_path": "/home/someone/x"}))
        self.assertSilent(self.call("mcp__other__create_note", {"body": "private-scratchbook"}))

    def test_without_a_setting_a_publishing_call_goes_through_and_says_that_it_did_not_check(self):
        result = self.bash(f"gh pr create --title t --body-file {self.dirty}", self.environment)
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn("not checked", result.stdout)

    def test_a_setting_whose_list_cannot_be_read_blocks_a_publishing_call(self):
        result = self.bash(f"gh pr create --title t --body-file {self.clean}",
                           self.named(self.directory / "missing.txt"))
        self.assertBlocked(result, "cannot be read")

    def test_hook_input_that_is_not_json_blocks_only_where_a_list_is_named(self):
        self.assertEqual(2, self.check(["--tool-call"], "not json").returncode)
        self.assertEqual(0, self.check(["--tool-call"], "not json", self.environment).returncode)


class ClaudeSettingsTest(Sandbox):
    """The tool hook as .claude/settings.json wires it, run the way Claude Code runs a command hook"""

    def setUp(self):
        super().setUp()
        self.settings = json.loads(SETTINGS.read_text(encoding="utf-8"))
        self.dirty = self.write("body.md", "Summary\n\nsee private-scratchbook\n")

    def hooks_for(self, tool_name):
        return [hook for group in self.settings.get("hooks", {}).get("PreToolUse", [])
                if matcher_matches(group.get("matcher", ""), tool_name) for hook in group["hooks"]]

    def test_the_committed_settings_carry_the_hook_and_nothing_personal(self):
        self.assertLessEqual(set(self.settings), {"$schema", "hooks"})

    def test_the_hook_runs_for_bash_and_the_github_mcp_tools_and_for_nothing_else(self):
        for tool_name in ("Bash", "mcp__github__create_pull_request", "mcp__github__add_issue_comment"):
            with self.subTest(tool_name=tool_name):
                self.assertEqual(1, len(self.hooks_for(tool_name)))
        for tool_name in ("Read", "Edit", "Write", "mcp__other__create_note"):
            with self.subTest(tool_name=tool_name):
                self.assertEqual([], self.hooks_for(tool_name))

    def test_a_hook_that_fails_blocks_the_call(self):
        for tool_name in ("Bash", "mcp__github__create_pull_request"):
            hook = self.hooks_for(tool_name)[0]
            self.assertEqual(("command", "block"), (hook.get("type"), hook.get("onFailure")))

    def test_the_committed_hook_blocks_a_call_that_names_the_private_notes(self):
        for tool_name, tool_input in (
                ("Bash", {"command": f"gh pr create --title t --body-file {self.dirty}"}),
                ("mcp__github__create_pull_request", {"owner": "o", "repo": "r", "title": "private-scratchbook"})):
            with self.subTest(tool_name=tool_name):
                environment = self.named()
                environment["CLAUDE_PROJECT_DIR"] = str(ROOT)
                payload = {"session_id": "test", "cwd": str(self.directory), "hook_event_name": "PreToolUse",
                           "tool_name": tool_name, "tool_input": tool_input}
                result = subprocess.run(["sh", "-c", self.hooks_for(tool_name)[0]["command"]],
                                        input=json.dumps(payload), capture_output=True, text=True,
                                        env=environment, cwd=str(self.directory))
                self.assertEqual(2, result.returncode, result.stderr)
                self.assertIn("blocked", result.stderr)


class PrePushHookTest(Sandbox):
    """.githooks/pre-push in a clone, through a real git push to a bare repository"""

    def setUp(self):
        super().setUp()
        self.repository = Repository(self)
        self.repository.commit({"README.md": "a project\n"})
        self.repository.publish()
        # the hook and the two scripts it runs, where a clone has them; untracked, so no push carries them
        for name in (".githooks/pre-push", "scripts/private_references.py", "scripts/pr_state_guard.py"):
            target = self.repository.work / name
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(ROOT / name, target)
        (self.repository.work / ".git" / "info" / "exclude").write_text(".githooks/\nscripts/\n", encoding="utf-8")
        self.repository.git("config", "core.hooksPath", ".githooks")

    def push(self, branch, environment):
        return subprocess.run(["git", "push", "origin", f"HEAD:refs/heads/{branch}"], capture_output=True,
                              text=True, env=environment, cwd=str(self.repository.work))

    def test_a_push_that_names_the_private_notes_is_refused_before_it_reaches_the_remote(self):
        head = self.repository.commit({"notes.md": "see private-scratchbook\n"})
        result = self.push("feature/x", self.named())
        self.assertNotEqual(0, result.returncode, result.stderr)
        self.assertIn(f"commit {head[:12]} notes.md line 1", result.stderr)
        self.assertFalse(self.repository.on_remote("feature/x"))

    def test_a_clean_push_reaches_the_remote_and_says_what_was_checked(self):
        self.repository.commit({"notes.md": "nothing private\n"})
        result = self.push("feature/x", self.named())
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn("1 added line and 1 commit message checked against 3 patterns, none found", result.stderr)
        self.assertTrue(self.repository.on_remote("feature/x"))

    def test_without_a_setting_the_push_reaches_the_remote_and_says_that_it_was_not_checked(self):
        self.repository.commit({"notes.md": "see private-scratchbook\n"})
        result = self.push("feature/x", self.environment)
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn("not checked", result.stderr)
        self.assertTrue(self.repository.on_remote("feature/x"))


if __name__ == "__main__":
    unittest.main()
