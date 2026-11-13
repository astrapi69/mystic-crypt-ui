#!/usr/bin/env python3
"""Merges a pull request the way the rules describe, stopping at the first step that fails.

The Python port of merge-pr.sh (#867): the check evaluation is a pure function
(``judge``) over parsed rollup entries, tested against fixtures in
merge_pr_test.py, because the Bash/jq/awk pipeline is where NEUTRAL was
misclassified as failure. Standard library only; gh and git stay subprocess
calls. The history the guards come from: two pull requests were closed rather
than merged because a branch deletion chained with ';' ran after a failed
merge (#299, #312), one was merged while its build check was still running
(#317), and astrapi69/mystic-crypt#183 was merged before its required codecov
status existed (#493).

Every GitHub read and write goes through the REST API (gh api), never
through gh pr view/checks/merge: those use GraphQL, which a Claude Code
cloud session's proxy answers with 403, and the script must run the same
on a desktop and there (#872).

Usage: scripts/merge_pr.py <pr-number> [target-branch]
"""

import json
import os
import re
import subprocess
import sys
import time
from dataclasses import dataclass
from pathlib import Path

# the script's own lines and the subprocesses' output interleave in order only unbuffered
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(line_buffering=True)

POLL_SECONDS = 20
INFORMATIONAL = {"NEUTRAL", "SKIPPED"}
PENDING_STATES = {"PENDING", "EXPECTED"}


@dataclass(frozen=True)
class RollupEntry:
    """One check run or commit status on the pull request's head."""

    name: str
    reported: bool
    conclusion: str


@dataclass(frozen=True)
class Verdict:
    """What the judgement decided and what it has to say."""

    mergeable: bool
    messages: tuple


def newest_runs(check_runs):
    """The newest run of each check name. A check that ran again on the same
    head - closing-references runs on "edited" - leaves its older run listed at
    the commit, and branch protection judges only the newest one (#970). GitHub
    numbers check runs in the order it creates them, so the highest id is the
    newest. A run without an id cannot be dated and is kept, so a red one among
    undated runs still blocks."""
    newest = {}
    undated = []
    for node in check_runs:
        run_id = node.get("id")
        if run_id is None:
            undated.append(node)
            continue
        name = node.get("name") or ""
        if name not in newest or run_id > newest[name]["id"]:
            newest[name] = node
    return list(newest.values()) + undated


def parse_checks(check_runs, statuses):
    """Parses what REST reports on the head: check runs from
    /commits/{sha}/check-runs (name, status, conclusion - lowercase there) and
    commit statuses from /commits/{sha}/status (context, state; that combined
    endpoint already keeps only the latest per context). Both are read, or a
    status such as codecov would look pending forever (#493, #872). Of the check
    runs, only the newest of each name counts (#970)."""
    entries = []
    for node in newest_runs(check_runs):
        name = node.get("name") or ""
        if not name:
            continue
        reported = node.get("status") == "completed"
        conclusion = (node.get("conclusion") or "PENDING").upper()
        entries.append(RollupEntry(name, reported, conclusion))
    for node in statuses:
        name = node.get("context") or ""
        if not name:
            continue
        state = (node.get("state") or "pending").upper()
        entries.append(RollupEntry(name, state not in PENDING_STATES, state))
    return entries


def pr_state(pull):
    """OPEN, MERGED or CLOSED, as the refusals name it: REST says state
    open|closed plus a merged flag (#872)."""
    if pull.get("merged"):
        return "MERGED"
    return (pull.get("state") or "").upper()


def is_cross_repository(pull):
    """Whether the head branch lives outside the base repository. A deleted
    fork reports no head repository and counts as cross: there is nothing
    this script should delete (#872)."""
    head_repo = (pull.get("head") or {}).get("repo")
    if not head_repo:
        return True
    base_repo = (pull.get("base") or {}).get("repo") or {}
    return head_repo.get("full_name") != base_repo.get("full_name")


def judge(entries, required_contexts, allow_optional_failure=False):
    """The decision engine of #867, decided by the maintainer on 2026-11-11.

    Every REQUIRED context has to have reported SUCCESS - every entry of its
    name, so a red duplicate run blocks (branch protection judges the newest
    run, which the rollup does not date; all-green is the conservative
    reading). A non-required entry reporting NEUTRAL or SKIPPED is named and
    does not block. A red non-required entry blocks by default, because
    GitGuardian is not a required check here and a red GitGuardian can mean a
    secret in the commits (AG-090); allow_optional_failure downgrades those to
    a loud warning (MERGE_PR_ALLOW_OPTIONAL_FAILURE=1 - the maintainer sets
    it, never an agent; decided 2026-11-11, written into AG-077).
    """
    messages = []
    if not entries:
        return Verdict(False, ("no checks at all - a pull request nothing ran on is not green",))

    required = [context for context in required_contexts if context]
    by_name = {}
    for entry in entries:
        by_name.setdefault(entry.name, []).append(entry)

    unreported = [
        context
        for context in required
        if context not in by_name or not all(entry.reported for entry in by_name[context])
    ]
    if unreported:
        return Verdict(False, ("required but not reported: " + ", ".join(unreported),))

    failed_required = []
    failed_optional = []
    informational = []
    still_running = []
    for entry in entries:
        if entry.name in required:
            if entry.conclusion != "SUCCESS":
                failed_required.append(f"{entry.name} ({entry.conclusion})")
        elif not entry.reported:
            still_running.append(entry.name)
        elif entry.conclusion == "SUCCESS":
            pass
        elif entry.conclusion in INFORMATIONAL:
            informational.append(f"{entry.name} ({entry.conclusion})")
        else:
            failed_optional.append(f"{entry.name} ({entry.conclusion})")

    if informational:
        messages.append("non-required, no verdict (not blocking): " + ", ".join(informational))
    if failed_required:
        messages.append("required checks not SUCCESS: " + ", ".join(failed_required))
        return Verdict(False, tuple(messages))
    if still_running:
        messages.append("non-required checks still running: " + ", ".join(still_running))
        return Verdict(False, tuple(messages))
    if failed_optional:
        if allow_optional_failure:
            messages.append(
                "WARNING: non-required checks failed and MERGE_PR_ALLOW_OPTIONAL_FAILURE=1"
                " overrides them: " + ", ".join(failed_optional))
        else:
            messages.append(
                "non-required checks failed: " + ", ".join(failed_optional)
                + " - a red GitGuardian can mean a secret (AG-090);"
                " MERGE_PR_ALLOW_OPTIONAL_FAILURE=1 overrides deliberately,"
                " and only the maintainer sets it, never an agent")
            return Verdict(False, tuple(messages))

    messages.append("every required check is SUCCESS (" + ", ".join(required) + ")")
    return Verdict(True, tuple(messages))


def all_reported(entries, required_contexts):
    """Whether the wait may end: every entry on the head has reported, and
    every required context appears at all (a check not created yet is not in
    the rollup - the codecov lesson of #493)."""
    names = {entry.name for entry in entries}
    if any(context and context not in names for context in required_contexts):
        return False
    return all(entry.reported for entry in entries)


def failure_of(failed):
    """The reason a look at the checks failed, in one line"""
    if isinstance(failed, json.JSONDecodeError):
        return f"the answer was not JSON: {failed.msg}"
    lines = [line.strip() for line in (failed.stderr or "").splitlines() if line.strip()]
    return lines[-1] if lines else f"exit status {failed.returncode}"


def wait_for_checks(poll, required_contexts, wait_seconds, poll_seconds=POLL_SECONDS,
                    sleep=time.sleep):
    """Looks at the head until the wait may end (all_reported) or the limit is
    reached. poll returns the entries of one look. Returns (entries, None) when
    the wait ended, or (None, refusal) when the limit came first.

    A look that fails - a GitHub call that ends with an error, an answer that is
    not JSON - is no answer: the wait says so on one line and looks again at the
    next poll. At the limit it refuses and names the failure, so an API that
    never answers still ends in "not merging", never in a merge (#971)."""
    waited = 0
    while True:
        try:
            entries = poll()
            failure = None
        except (subprocess.CalledProcessError, json.JSONDecodeError) as failed:
            entries = []
            failure = failure_of(failed)
            print(f"the look at the checks failed ({failure}); looking again in {poll_seconds}s")
        if entries and all_reported(entries, required_contexts):
            return entries, None
        if waited >= wait_seconds:
            if failure:
                return None, (f"after {waited}s - the last look at the checks failed ({failure})"
                              " - not merging")
            running = ", ".join(entry.name for entry in entries if not entry.reported) or "none"
            missing = ", ".join(context for context in required_contexts
                                if context not in {entry.name for entry in entries}) or "none"
            return None, (f"after {waited}s - still running: {running}; required but not"
                          f" reported: {missing} - not merging")
        sleep(poll_seconds)
        waited += poll_seconds


def base_mismatch(base, target):
    """gh merges into the pull request's base; the local update goes to target.
    When the two differ the merge lands where nobody is looking (#395)."""
    if base == target:
        return None
    return (f"pull request targets '{base}', but the merge target given here is '{target}'"
            " - one of the two is wrong, and merging would put the change where nobody is looking")


def dirty_tree_note(porcelain_output):
    """A dirty tree is somebody's work in progress (#340); one line names it."""
    if not porcelain_output.strip():
        return None
    return ("note: this worktree has uncommitted changes. They are not touched - the merge does not"
            " change the checked-out branch - but check that you meant to merge right now")


TITLE_PATTERN = re.compile(
    r"^(feat|fix|refactor|docs|test|chore|build|perf|ci|style|revert)(\([^)]+\))?!?: \S")


def conventional_title_error(title):
    """The pull request title becomes the squash commit subject, so it has to
    be a Conventional Commit (squash pilot, decided by the maintainer on
    2026-11-11); returns why it is not, or None."""
    if not TITLE_PATTERN.match(title or ""):
        return ("the pull request title is not a Conventional Commit, and under the squash"
                " pilot it becomes the commit subject on develop - rename the pull request"
                " (feat|fix|refactor|docs|test|chore|build|perf|ci|style|revert, optional"
                " (scope), then ': ' and the sentence), then merge again")
    if "(#" in title:
        return ("the pull request title carries an issue number - the merge appends the pull"
                " request number itself, and the issue belongs in the pull request text as"
                " Closes #<n>: rename the pull request, then merge again")
    return None


def feat_freeze_error(title, freeze_path):
    """While the file feat-freeze.md lies in the repository root, develop takes
    only fix, test, docs, build and chore (the maintainer's bugfix-only
    decision of 2026-11-13, until the RELEASE-8.15 tag, which removes the
    file); returns why a feat pull request is refused, or None."""
    match = TITLE_PATTERN.match(title or "")
    if not match or match.group(1) != "feat":
        return None
    if not freeze_path.is_file():
        return None
    return ("the repository is bugfix-only until the RELEASE-8.15 tag (feat-freeze.md, the"
            " maintainer's decision of 2026-11-13): a feat pull request waits until the tag"
            " removes that file, or the maintainer lifts the freeze")


FRAGMENT_SECTIONS = ("security", "added", "changed", "fixed", "deprecated", "removed")
FRAGMENT_REQUIRED_TYPES = ("feat", "fix", "perf")
FRAGMENT_SECTION_OF_TYPE = {"feat": "added", "fix": "fixed", "perf": "changed"}


def fragment_error(title, pr, changed_paths):
    """A user-visible change brings its CHANGELOG entry along as a fragment in
    the same pull request, so it lands in the same squash commit: feat, fix,
    perf and every breaking change need one, chore, test, refactor, ci, build
    and docs do not (changelog.d/README.md). Returns why the merge is refused,
    or None."""
    match = TITLE_PATTERN.match(title or "")
    if not match:
        return None
    change_type = match.group(1)
    breaking = "!" in (title or "").split(":", 1)[0]
    if change_type not in FRAGMENT_REQUIRED_TYPES and not breaking:
        return None
    prefix = f"changelog.d/{pr}."
    for path in changed_paths:
        if path.startswith(prefix) and path.endswith(".md") \
                and path[len(prefix):-3] in FRAGMENT_SECTIONS:
            return None
    section = "changed" if breaking else FRAGMENT_SECTION_OF_TYPE[change_type]
    return (f"a {change_type}{'!' if breaking else ''} pull request brings its CHANGELOG entry"
            f" along as a fragment, and this one has none: add changelog.d/{pr}.{section}.md"
            " (one human-written bullet, see changelog.d/README.md), push, and merge again")


def squash_subject(title, pr):
    """The squash commit subject: the pull request title, with the pull
    request number appended when the title does not carry it already."""
    marker = f"(#{pr})"
    return title if marker in title else f"{title} {marker}"


def merge_command(pr, head_sha, subject, body):
    """The merge squashes to one commit on develop (the squash pilot: one
    change, one commit) and pins the judged head: the sha parameter of
    PUT /pulls/{n}/merge makes GitHub refuse when the head moved between
    judgement and merge, which is what --match-head-commit did before the
    REST port (#872). The subject is the pull request title with its
    number, the body the pull request body, which carries the why (AG-067).
    The branch is never deleted by the merge call itself: with the head
    branch checked out locally, gh pr merge --delete-branch switched
    branches and pulled under the running script (#496)."""
    return ["gh", "api", "-X", "PUT", f"repos/{{owner}}/{{repo}}/pulls/{pr}/merge",
            "-f", "merge_method=squash", "-f", f"sha={head_sha}",
            "-f", f"commit_title={subject}", "-f", f"commit_message={body}"]


def resolve_required(target, fetch):
    """The required contexts of the target's branch protection; a hotfix branch
    without protection of its own borrows develop's (#395). A target without
    required checks is refused: nothing then says what green means (#493).
    ``fetch`` maps a branch name to its contexts list or None."""
    contexts = fetch(target)
    note = None
    if not contexts and target.startswith("hotfix/"):
        contexts = fetch("develop")
        if contexts:
            note = f"note: {target} has no protection of its own - waiting for develop's required checks"
    return contexts or [], note


def run(argv):
    gh_json = lambda *args: json.loads(subprocess.run(
        ["gh"] + list(args), check=True, capture_output=True, text=True).stdout)

    if not argv or len(argv) > 2:
        die("usage: scripts/merge_pr.py <pr-number> [target-branch]")
    pr = argv[0]
    target = argv[1] if len(argv) > 1 else "develop"
    wait_seconds = int(os.environ.get("MERGE_PR_WAIT_SECONDS", "2400"))
    allow_optional_failure = os.environ.get("MERGE_PR_ALLOW_OPTIONAL_FAILURE") == "1"

    porcelain = subprocess.run(["git", "status", "--porcelain"], check=True,
                               capture_output=True, text=True).stdout
    note = dirty_tree_note(porcelain)
    if note:
        print(note)

    pull = gh_json("api", f"repos/{{owner}}/{{repo}}/pulls/{pr}")
    state = pr_state(pull)
    if state != "OPEN":
        die(f"pull request #{pr} is {state}, not OPEN")
    mismatch = base_mismatch(pull["base"]["ref"], target)
    if mismatch:
        die(mismatch)
    title = pull.get("title") or ""
    title_error = conventional_title_error(title)
    if title_error:
        die(title_error)
    repo_root = subprocess.run(["git", "rev-parse", "--show-toplevel"], check=True,
                               capture_output=True, text=True).stdout.strip()
    freeze_error = feat_freeze_error(title, Path(repo_root) / "feat-freeze.md")
    if freeze_error:
        die(freeze_error)
    files_lines = subprocess.run(
        ["gh", "api", f"repos/{{owner}}/{{repo}}/pulls/{pr}/files", "--paginate",
         "-q", ".[].filename"],
        check=True, capture_output=True, text=True).stdout
    changed_paths = [line for line in files_lines.splitlines() if line]
    missing_fragment = fragment_error(title, pr, changed_paths)
    if missing_fragment:
        die(missing_fragment)
    head_sha = pull["head"]["sha"]

    def fetch_contexts(branch):
        result = subprocess.run(
            ["gh", "api", f"repos/{{owner}}/{{repo}}/branches/{branch}/protection/required_status_checks",
             "-q", ".contexts"],
            capture_output=True, text=True)
        if result.returncode != 0:
            return None
        return json.loads(result.stdout)

    required, protection_note = resolve_required(target, fetch_contexts)
    if protection_note:
        print(protection_note)
    if not required:
        die(f"'{target}' has no required checks in its branch protection - nothing says"
            " what green means for a merge there, so this does not merge (#493)")
    print("required on " + target + ": " + ", ".join(required))

    def poll():
        check_lines = subprocess.run(
            ["gh", "api", f"repos/{{owner}}/{{repo}}/commits/{head_sha}/check-runs", "--paginate",
             "-q", ".check_runs[] | {id, name, status, conclusion}"],
            check=True, capture_output=True, text=True).stdout
        check_runs = [json.loads(line) for line in check_lines.splitlines() if line]
        statuses = gh_json("api", f"repos/{{owner}}/{{repo}}/commits/{head_sha}/status").get(
            "statuses") or []
        return parse_checks(check_runs, statuses)

    print(f"waiting for the checks on #{pr} (up to {wait_seconds // 60} minutes)")
    entries, refusal = wait_for_checks(poll, required, wait_seconds)
    if refusal:
        die(refusal)

    verdict = judge(entries, required, allow_optional_failure)
    for message in verdict.messages:
        print(message)
    if not verdict.mergeable:
        for entry in entries:
            print(f"  {entry.name}: {entry.conclusion}{'' if entry.reported else ' (running)'}")
        die(f"not merging #{pr}")

    subject = squash_subject(title, pr)
    subprocess.run(merge_command(pr, head_sha, subject, pull.get("body") or ""),
                   check=True, capture_output=True)

    head = pull["head"]["ref"]
    if is_cross_repository(pull):
        print(f"the head branch '{head}' lives in a fork - not deleted from here")
    else:
        subprocess.run(["gh", "api", "-X", "DELETE", f"repos/{{owner}}/{{repo}}/git/refs/heads/{head}",
                        "--silent"], capture_output=True)
        gone = subprocess.run(["gh", "api", f"repos/{{owner}}/{{repo}}/branches/{head}", "--silent"],
                              capture_output=True).returncode != 0
        if gone:
            print(f"the remote branch '{head}' is gone; the local one is left as it is")
        else:
            print(f"note: the remote branch '{head}' is still there - delete it by hand if it is not needed")

    subprocess.run(["git", "fetch", "--quiet", "origin"], check=True)
    tip = subprocess.run(["git", "rev-parse", "--short", f"origin/{target}"], check=True,
                         capture_output=True, text=True).stdout.strip()
    subject = subprocess.run(["git", "log", "-1", "--format=%s", f"origin/{target}"], check=True,
                             capture_output=True, text=True).stdout.strip()
    print(f"merged #{pr} - origin/{target} is now {tip}: {subject}")
    # The local copy of the target follows the merge without moving HEAD and without touching
    # uncommitted work, wherever the target is checked out (#340, #496, #543)
    script_dir = os.path.dirname(os.path.abspath(__file__))
    subprocess.run([os.path.join(script_dir, "update-local-branch.sh"), target], check=True)


def die(message):
    print(f"merge-pr: {message}", file=sys.stderr)
    sys.exit(1)


if __name__ == "__main__":
    run(sys.argv[1:])
