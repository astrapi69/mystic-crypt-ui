#!/usr/bin/env python3
"""Refuses a push, and blocks a pull request, issue or comment text, that names the private notes (#1103).

The maintainer's private notes hold what this public repository must not carry: their own name and
the paths inside them, absolute paths of his machines, the ids of the private part of the rules.
Which strings those are is private too, so this repository holds only the check. The patterns live
in a list outside it, which a local setting names:

  1. the environment variable MYSTIC_CRYPT_PRIVATE_REFERENCES, which wins, or else
  2. the git config key mysticcrypt.privateReferences (read with --type=path, so ~ works).

The list holds one Python regular expression per line, searched in every line of a text. A line
whose first character that is not a blank is # is a comment, blank lines are skipped, and the
whitespace around a pattern is not part of it.

- Without a setting nothing is checked, and the check says so: "not checked". A clone without the
  private notes has no private reference to leak (the maintainer's decision of 2026-11-24).
- A setting whose list cannot be read, holds no pattern or holds one that is no regular expression
  refuses: a check that cannot check does not report green.
- A clean run says what it checked, so "none found" and "nothing checked" never read the same.
- A finding is named by where it is and the line of the list that found it, never by the pattern or
  the text it matched: the message can end up in a transcript or a report.

Three entry points:

  --push [<remote> <url>]  the pre-push hook (.githooks/pre-push): git's ref lines on standard
                           input. Checks the added lines and the message of every commit the push
                           adds, a commit the remote already has excluded. Exit 1 refuses the push.
  --file <path>...         checks files, for example a body file before gh. Exit 1 on a finding.
  --tool-call              the PreToolUse hook of .claude/settings.json: Claude Code's hook input
                           on standard input. Checks what a gh call (a title, a body, a body file)
                           or a write tool of the GitHub MCP server would publish. Exit 2 blocks the
                           call, which is Claude Code's exit code for that; a write into a repository
                           the list itself names is not checked, since that is the private notes.
"""

import json
import os
import re
import shlex
import subprocess
import sys

NAME = "private references"
VARIABLE = "MYSTIC_CRYPT_PRIVATE_REFERENCES"
GIT_KEY = "mysticcrypt.privateReferences"
ZERO = "0" * 40
NOT_CHECKED = (f"{NAME}: not checked, no pattern list is named (the variable {VARIABLE} or the git config "
               f"key {GIT_KEY})")
BLOCK = 2

HUNK = re.compile(r"^(@@+) (?:-\d+(?:,\d+)? )+\+(\d+)(?:,\d+)? @@")
PUNCTUATION = set("();<>|&\n")
SHELLS = {"bash", "sh", "zsh", "dash"}
GH_TEXT_GROUPS = {"pr", "issue", "release"}
GH_TEXT_OPTIONS = {"--title", "-t", "--body", "-b", "--notes", "-n", "--subject"}
GH_FILE_OPTIONS = {"--body-file", "-F", "--notes-file"}
GH_REPOSITORY_OPTIONS = {"--repo", "-R"}
API_FIELD_OPTIONS = {"-f": False, "--raw-field": False, "-F": True, "--field": True}
API_VALUE_OPTIONS = {"-X", "--method", "-H", "--header", "-q", "--jq", "-t", "--template", "--hostname",
                     "-p", "--preview", "--cache", "--input"}
MCP_GITHUB = "mcp__github__"
MCP_READ = re.compile(r"^mcp__github__(?:(?:get|list|search)_.*|.*_(?:read|get|list))$")


class Refusal(Exception):
    """The check cannot check: the list, a text or what a push adds cannot be read"""


def counted(number, word):
    return f"{number} {word}" + ("" if number == 1 else "s")


def located_setting(environ, cwd):
    """(what names the list, the path it names), or None where nothing names one"""
    if VARIABLE in environ:
        return f"the variable {VARIABLE}", os.path.expanduser(environ[VARIABLE])
    try:
        result = subprocess.run(["git", "config", "--type=path", "--get", GIT_KEY], capture_output=True,
                                text=True, cwd=cwd)
    except FileNotFoundError:
        return None
    if result.returncode == 1:
        return None
    if result.returncode:
        raise Refusal(f"the git config key {GIT_KEY} cannot be read: {result.stderr.strip()[:300]}")
    return f"the git config key {GIT_KEY}", result.stdout.rstrip("\n")


def load_patterns(source, path):
    """[(line of the list, compiled pattern)] of the list the setting names"""
    try:
        with open(path, encoding="utf-8") as handle:
            lines = handle.read().splitlines()
    except OSError as failure:
        raise Refusal(f"the pattern list that {source} names cannot be read ({reason(failure)})")
    except UnicodeDecodeError:
        raise Refusal(f"the pattern list that {source} names cannot be read (it is not UTF-8)")
    compiled = []
    for number, line in enumerate(lines, 1):
        pattern = line.strip()
        if not pattern or pattern.startswith("#"):
            continue
        try:
            compiled.append((number, re.compile(pattern)))
        except re.error as failure:
            raise Refusal(f"line {number} of the pattern list that {source} names is no regular expression "
                          f"({failure.msg})")
    if not compiled:
        raise Refusal(f"the pattern list that {source} names holds no pattern")
    return compiled


def findings_in_line(where, number, line, patterns):
    return [f"{where} line {number}: pattern on line {list_line} of the list"
            for list_line, pattern in patterns if pattern.search(line)]


def findings_in_text(where, text, patterns):
    """(findings, number of lines) of one text"""
    lines = text.splitlines()
    findings = []
    for number, line in enumerate(lines, 1):
        findings.extend(findings_in_line(where, number, line, patterns))
    return findings, len(lines)


def reason(failure):
    """Why a file could not be opened, without its path: the path of the list is private too"""
    return failure.strerror or type(failure).__name__


def read_text(path, what):
    try:
        with open(path, encoding="utf-8", errors="replace") as handle:
            return handle.read()
    except OSError as failure:
        raise Refusal(f"{what} cannot be read ({reason(failure)})")


# --push


def git(*arguments):
    result = subprocess.run(["git", "-c", "core.quotePath=false", *arguments], capture_output=True, text=True,
                            errors="replace")
    if result.returncode:
        raise Refusal(f"git {arguments[0]} failed, so what the push adds cannot be told: "
                      f"{(result.stderr or result.stdout).strip()[:300]}")
    return result.stdout


def pushed_refs(stdin):
    """(local sha, remote sha or None for a new ref) of every ref the push writes, deletions left out"""
    for line in stdin:
        parts = line.split()
        if len(parts) != 4 or parts[1] == ZERO:
            continue
        yield parts[1], None if parts[3] == ZERO else parts[3]


def known_commit(sha):
    return subprocess.run(["git", "cat-file", "-e", f"{sha}^{{commit}}"], capture_output=True).returncode == 0


def remote_branches(remote_name):
    """The remote-tracking branches that stand for what the remote has: those of the remote when git
    knows it by that name, all of them when the push names a URL instead"""
    return f"--remotes={remote_name}" if remote_name in git("remote").split() else "--remotes"


def commits_added(local, remote, remote_branches_option):
    """The commits of local that neither the remote ref nor a remote-tracking branch of the remote has"""
    excluded = [remote] if remote and known_commit(remote) else []
    return git("rev-list", "--reverse", local, "--not", *excluded, remote_branches_option).split()


def added_lines(sha):
    """(path, line number, text) of every line the commit adds; of a merge, only what it adds beyond all
    of its parents (the combined diff), since what a parent brings is checked as that parent's commit"""
    parents = len(git("show", "-s", "--format=%P", sha).split())
    arguments = ["diff-tree", "-r", "-p", "-U0", "-M", "--no-color", "--no-ext-diff", "--no-commit-id", "--root"]
    if parents > 1:
        arguments.append("--cc")
    width, path, number, in_header = 1, None, 0, False
    for line in git(*arguments, sha).splitlines():
        if line.startswith("diff "):
            in_header, path = True, None
            continue
        hunk = HUNK.match(line)
        if hunk:
            in_header = False
            width, number = len(hunk.group(1)) - 1, int(hunk.group(2))
            continue
        if in_header:
            if line.startswith("+++ "):
                target = line[4:]
                path = None if target == "/dev/null" else target[2:] if target.startswith("b/") else target
            continue
        prefix = line[:width]
        if path is None or line.startswith("\\") or "-" in prefix:
            continue
        if set(prefix) == {"+"}:
            yield path, number, line[width:]
        number += 1


def check_push(arguments, stdin, out, environ):
    # read to the end first, so the hook that writes the lines never writes into a closed pipe
    ref_lines = stdin.read().splitlines()
    try:
        setting = located_setting(environ, None)
        if setting is None:
            print(NOT_CHECKED, file=out)
            return 0
        patterns = load_patterns(*setting)
        remote_option = remote_branches(arguments[0] if arguments else None)
        commits = []
        for local, remote in pushed_refs(ref_lines):
            for sha in commits_added(local, remote, remote_option):
                if sha not in commits:
                    commits.append(sha)
        findings, lines = [], 0
        for sha in commits:
            for path, number, text in added_lines(sha):
                lines += 1
                findings.extend(findings_in_line(f"commit {sha[:12]} {path}", number, text, patterns))
            found, _ = findings_in_text(f"commit {sha[:12]} message", git("show", "-s", "--format=%B", sha), patterns)
            findings.extend(found)
    except Refusal as failure:
        print(f"{NAME}: push refused, {failure}", file=out)
        return 1
    if not commits:
        print(f"{NAME}: nothing to check, the push adds no commit the remote does not have", file=out)
        return 0
    if findings:
        print(f"{NAME}: push refused, {counted(len(findings), 'reference')} to the private notes:", file=out)
        for finding in findings:
            print(f"  {finding}", file=out)
        print("  None of these commits is pushed yet: rewrite them so that no commit carries the reference "
              "(a new commit on top leaves it readable), then push again.", file=out)
        return 1
    print(f"{NAME}: {counted(lines, 'added line')} and {counted(len(commits), 'commit message')} checked against "
          f"{counted(len(patterns), 'pattern')}, none found", file=out)
    return 0


# --file


def check_files(paths, out, environ):
    try:
        setting = located_setting(environ, None)
        if setting is None:
            print(NOT_CHECKED, file=out)
            return 0
        patterns = load_patterns(*setting)
        if not paths:
            raise Refusal("no file was named")
        findings, lines = [], 0
        for path in paths:
            found, count = findings_in_text(path, read_text(path, path), patterns)
            findings.extend(found)
            lines += count
    except Refusal as failure:
        print(f"{NAME}: refused, {failure}", file=out)
        return 1
    if findings:
        print(f"{NAME}: {counted(len(findings), 'reference')} to the private notes:", file=out)
        for finding in findings:
            print(f"  {finding}", file=out)
        return 1
    print(f"{NAME}: {counted(len(paths), 'file')} of {counted(lines, 'line')} checked against "
          f"{counted(len(patterns), 'pattern')}, none found", file=out)
    return 0


# --tool-call


class Invocation:
    """One call that publishes texts: where they go, and the texts as (label, kind, value), kind being
    "text" (the value is the text), "file" (the value is a path) or "stdin" (a text nobody can see)"""

    def __init__(self, targets, items):
        self.targets = targets
        self.items = items


def words_of(command):
    lexer = shlex.shlex(command, posix=True, punctuation_chars="".join(sorted(PUNCTUATION)))
    lexer.whitespace = " \t\r"
    lexer.whitespace_split = True
    lexer.commenters = ""
    return list(lexer)


def is_operator(word):
    return bool(word) and set(word) <= PUNCTUATION


def is_separator(word):
    return is_operator(word) and not set(word) & set("<>")


def gh_invocations(command, cwd, depth=0):
    """Every gh call in a shell command that publishes a text, a bash -c inside it included"""
    if not re.search(r"\bgh\b", command):
        return []
    try:
        words = words_of(command)
    except ValueError as failure:
        raise Refusal(f"the command cannot be split into words ({failure}), so the gh call in it cannot be "
                      f"checked; give gh its text in a file of its own")
    invocations = []
    index = 0
    while index < len(words):
        if is_separator(words[index]):
            index += 1
            continue
        end = index
        while end < len(words) and not is_separator(words[end]):
            end += 1
        segment = words[index:end]
        if segment[0] == "cd" and len(segment) > 1:
            cwd = resolved(cwd, segment[1])
        else:
            invocations.extend(segment_invocations(segment, cwd, depth))
        index = end
    return invocations


def segment_invocations(segment, cwd, depth):
    """The gh call of one simple command, also behind env, sudo, time and the like, or the gh calls of
    the script a shell runs with -c"""
    for position, word in enumerate(segment):
        if word == "gh" or word.endswith("/gh"):
            invocation = gh_invocation(segment[position + 1:], cwd)
            return [invocation] if invocation else []
        if os.path.basename(word) in SHELLS and depth < 5:
            script = shell_script(segment[position + 1:])
            if script is not None:
                return gh_invocations(script, cwd, depth + 1)
    return []


def shell_script(arguments):
    """The command string of a shell started with -c (also within -lc, -ec and the like), or None"""
    for position, argument in enumerate(arguments[:-1]):
        if argument.startswith("-") and not argument.startswith("--") and "c" in argument[1:]:
            return arguments[position + 1]
    return None


def option_value(words, index, options):
    """(option, value, next index) when words[index] is one of the options with its value, else None"""
    word = words[index]
    if word.startswith("--") and "=" in word:
        option, value = word.split("=", 1)
        return (option, value, index + 1) if option in options else None
    if word in options:
        return (word, words[index + 1], index + 2) if index + 1 < len(words) else None
    if not word.startswith("--") and word[:2] in options and len(word) > 2:
        return word[:2], word[2:], index + 1
    return None


def resolved(cwd, path):
    return os.path.normpath(os.path.join(cwd, os.path.expanduser(os.path.expandvars(path))))


def gh_invocation(words, cwd):
    """The texts one gh call publishes: a pr, issue or release command's title, body and notes, and the
    fields and input of gh api; None for a call that publishes nothing"""
    stdin_file = None
    for index, word in enumerate(words[:-1]):
        if word == "<":
            stdin_file = resolved(cwd, words[index + 1])
    positionals = [word for word in words if not word.startswith("-") and not is_operator(word)]
    group = positionals[0] if positionals else None
    if group == "api":
        return api_invocation(words, cwd, stdin_file)
    if group not in GH_TEXT_GROUPS:
        return None
    targets, items = [], []
    index = 0
    while index < len(words):
        found = option_value(words, index, GH_TEXT_OPTIONS | GH_FILE_OPTIONS | GH_REPOSITORY_OPTIONS)
        if not found:
            index += 1
            continue
        option, value, index = found
        if option in GH_REPOSITORY_OPTIONS:
            targets.append(value)
        elif option in GH_TEXT_OPTIONS:
            items.append((option, "text", value))
        else:
            items.append(file_item(option, value, cwd, stdin_file))
    return Invocation(targets, items) if items else None


def file_item(option, value, cwd, stdin_file):
    if value == "-":
        return (option, "file", stdin_file) if stdin_file else (option, "stdin", None)
    return option, "file", resolved(cwd, value)


def api_invocation(words, cwd, stdin_file):
    method, endpoint, items = None, None, []
    index = words.index("api") + 1
    while index < len(words):
        word = words[index]
        found = option_value(words, index, set(API_FIELD_OPTIONS) | API_VALUE_OPTIONS)
        if found:
            option, value, index = found
            if option in ("-X", "--method"):
                method = value.upper()
            elif option == "--input":
                items.append(file_item(option, value, cwd, stdin_file))
            elif option in API_FIELD_OPTIONS:
                key, _, field = value.partition("=")
                if API_FIELD_OPTIONS[option] and field.startswith("@"):
                    items.append(file_item(f"{option} {key}", field[1:], cwd, stdin_file))
                else:
                    items.append((f"{option} {key}", "text", field))
            continue
        if endpoint is None and not word.startswith("-") and not is_operator(word):
            endpoint = word
        index += 1
    if method == "GET" or not items:
        return None
    return Invocation([endpoint] if endpoint else [], items)


def strings_of(value, label=""):
    """(label, string) of every string in a JSON value, labelled by where it sits"""
    if isinstance(value, str):
        yield label, value
    elif isinstance(value, dict):
        for key, item in value.items():
            yield from strings_of(item, f"{label}.{key}" if label else str(key))
    elif isinstance(value, list):
        for position, item in enumerate(value):
            yield from strings_of(item, f"{label}[{position}]")


def mcp_invocation(tool_name, tool_input):
    """Every text a write tool of the GitHub MCP server carries; None for a read tool"""
    if MCP_READ.match(tool_name):
        return None
    owner, repository = tool_input.get("owner"), tool_input.get("repo")
    targets = []
    if isinstance(repository, str):
        targets.append(f"{owner}/{repository}" if isinstance(owner, str) else repository)
    items = [(label, "text", text) for label, text in strings_of(tool_input)]
    return Invocation(targets, items) if items else None


def invocations_of(raw):
    """What the call a PreToolUse hook input describes would publish, as invocations"""
    call = json.loads(raw)
    if not isinstance(call, dict) or not isinstance(call.get("tool_input"), dict):
        raise ValueError("the hook input is no JSON object with a tool_input object")
    tool_name, tool_input = call.get("tool_name"), call["tool_input"]
    if tool_name == "Bash":
        command = tool_input.get("command")
        if not isinstance(command, str):
            raise ValueError("the Bash call carries no command")
        cwd = call.get("cwd") if isinstance(call.get("cwd"), str) else os.getcwd()
        return gh_invocations(command, cwd)
    if isinstance(tool_name, str) and tool_name.startswith(MCP_GITHUB):
        invocation = mcp_invocation(tool_name, tool_input)
        return [invocation] if invocation else []
    return []


def check_texts(invocations, patterns):
    """(findings, texts, lines, skipped) over every invocation a pattern list does not name as its target"""
    findings, texts, lines, skipped = [], 0, 0, 0
    for invocation in invocations:
        if any(pattern.search(target) for target in invocation.targets for _, pattern in patterns):
            skipped += 1
            continue
        for label, kind, value in invocation.items:
            if kind == "stdin":
                raise Refusal(f"{label} takes its text from standard input, which this check cannot see; give it a "
                              f"file instead")
            if kind == "file":
                label = f"{label} {value}"
                value = read_text(value, f"the file of {label}")
            found, count = findings_in_text(label, value, patterns)
            findings.extend(found)
            texts += 1
            lines += count
    return findings, texts, lines, skipped


def project_directory(environ):
    """The checkout the hook belongs to, whose git config may name the list; Claude Code sets it"""
    directory = environ.get("CLAUDE_PROJECT_DIR")
    return directory if directory and os.path.isdir(directory) else None


def check_tool_call(stdin, out, err, environ):
    try:
        invocations, unreadable = [], None
        try:
            invocations = invocations_of(stdin.read())
        except (ValueError, Refusal) as failure:
            unreadable = failure
        if unreadable is None and not invocations:
            return 0
        setting = located_setting(environ, project_directory(environ))
        if setting is None:
            if unreadable is None:
                print(NOT_CHECKED, file=out)
            return 0
        patterns = load_patterns(*setting)
        if unreadable is not None:
            raise Refusal(f"the call cannot be read, so what it would publish cannot be checked: {unreadable}")
        findings, texts, lines, skipped = check_texts(invocations, patterns)
    except Refusal as failure:
        print(f"{NAME}: call blocked, {failure}", file=err)
        return BLOCK
    if findings:
        print(f"{NAME}: call blocked, what it would publish names the private notes "
              f"({counted(len(findings), 'reference')}):", file=err)
        for finding in findings:
            print(f"  {finding}", file=err)
        print("  Write the text without the reference and call again.", file=err)
        return BLOCK
    if texts == 0 and skipped:
        print(f"{NAME}: not checked, the call writes into a repository the pattern list names", file=out)
        return 0
    print(f"{NAME}: {counted(texts, 'text')} of {counted(lines, 'line')} checked against "
          f"{counted(len(patterns), 'pattern')}, none found", file=out)
    return 0


USAGE = "usage: private_references.py --push [<remote> <url>] | --file <path>... | --tool-call"


def main(argv, stdin, out, err, environ):
    mode = argv[1] if len(argv) > 1 else None
    if mode == "--push":
        return check_push(argv[2:], stdin, err, environ)
    if mode == "--file":
        return check_files(argv[2:], err, environ)
    if mode == "--tool-call":
        return check_tool_call(stdin, out, err, environ)
    print(USAGE, file=err)
    return 1


if __name__ == "__main__":
    sys.exit(main(sys.argv, sys.stdin, sys.stdout, sys.stderr, os.environ))
