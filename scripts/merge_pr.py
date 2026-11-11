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

Usage: scripts/merge_pr.py <pr-number> [target-branch]
"""

import json
import os
import subprocess
import sys
import time
from dataclasses import dataclass

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


def parse_rollup(rollup):
    """Parses gh's statusCheckRollup: check runs carry name/status/conclusion,
    commit statuses context/state - both are read, or a status would look
    pending forever."""
    entries = []
    for node in rollup:
        name = node.get("name") or node.get("context") or ""
        if not name:
            continue
        if node.get("status") is not None:
            reported = node.get("status") == "COMPLETED"
        else:
            reported = (node.get("state") or "PENDING") not in PENDING_STATES
        conclusion = node.get("conclusion") or node.get("state") or "PENDING"
        entries.append(RollupEntry(name, reported, conclusion))
    return entries


def judge(entries, required_contexts):
    """The decision engine of #867, decided by the maintainer on 2026-11-11.

    Every REQUIRED context has to have reported SUCCESS - every entry of its
    name, so a red duplicate run blocks (branch protection judges the newest
    run, which the rollup does not date; all-green is the conservative
    reading). A non-required entry reporting NEUTRAL or SKIPPED is named and
    does not block. A red non-required entry ALWAYS blocks: GitGuardian is
    not a required check here, a red GitGuardian can mean a secret in the
    commits, and the standing merge grant is conditional on green checks
    (AG-078, AG-090) - there is no override.
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
        messages.append(
            "non-required checks failed: " + ", ".join(failed_optional)
            + " - a red check always blocks (AG-078, AG-090)")
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


def merge_command(pr, head_sha):
    """The merge pins the judged head: a push between judgement and merge must
    fail the merge, not slip through (--match-head-commit needs the full sha).
    Never --delete-branch: with the head branch checked out locally, gh
    switches branches and pulls under the running script (#496)."""
    return ["gh", "pr", "merge", str(pr), "--merge", "--match-head-commit", head_sha]


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

    porcelain = subprocess.run(["git", "status", "--porcelain"], check=True,
                               capture_output=True, text=True).stdout
    note = dirty_tree_note(porcelain)
    if note:
        print(note)

    view = gh_json("pr", "view", pr, "--json", "state,baseRefName,headRefName,headRefOid,isCrossRepository")
    if view["state"] != "OPEN":
        die(f"pull request #{pr} is {view['state']}, not OPEN")
    mismatch = base_mismatch(view["baseRefName"], target)
    if mismatch:
        die(mismatch)

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

    print(f"waiting for the checks on #{pr} (up to {wait_seconds // 60} minutes)")
    waited = 0
    while True:
        entries = parse_rollup(gh_json("pr", "view", pr, "--json", "statusCheckRollup")["statusCheckRollup"])
        if entries and all_reported(entries, required):
            break
        if waited >= wait_seconds:
            running = ", ".join(entry.name for entry in entries if not entry.reported) or "none"
            missing = ", ".join(context for context in required
                                if context not in {entry.name for entry in entries}) or "none"
            die(f"after {waited}s - still running: {running}; required but not reported: {missing}"
                " - not merging")
        time.sleep(POLL_SECONDS)
        waited += POLL_SECONDS

    verdict = judge(entries, required)
    for message in verdict.messages:
        print(message)
    if not verdict.mergeable:
        subprocess.run(["gh", "pr", "checks", pr])
        die(f"not merging #{pr}")

    head_sha = view["headRefOid"]
    subprocess.run(merge_command(pr, head_sha), check=True)

    head = view["headRefName"]
    if view["isCrossRepository"]:
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
