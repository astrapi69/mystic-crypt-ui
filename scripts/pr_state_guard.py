#!/usr/bin/env python3
"""pre-push: refuses a push to the branch of a pull request that is merged or closed (AG-061).

A pull request whose branch is pushed after its merge loses that push: the squash froze the branch,
and GitHub deleted it, so the push makes a new branch nobody reviews. It happened twice on
2026-11-19/20 (#1063 lost with its base, #1086 after its merge). Git calls this with the remote's
name and URL as arguments and one line per pushed ref on standard input:

    <local ref> <local sha> <remote ref> <remote sha>

For each branch pushed to GitHub it asks for the pull requests whose head that branch is. A branch
without one, or with an open one, goes through; one whose newest pull request is merged or closed
is refused, naming it. A deletion, a tag and the long-lived branches are not asked about. When
GitHub cannot be asked, the push is refused too: a check that cannot check does not report green.
`git push --no-verify` skips the hook; it is used only on the maintainer's instruction, and only
for the case that GitHub cannot be reached.

Installed with: make install-hooks   (.githooks/pre-push calls this)
usage: pr_state_guard.py <remote name> <remote URL>  < the lines git writes
"""

import json
import re
import subprocess
import sys

ZERO = "0" * 40
LONG_LIVED = {"develop", "master", "main"}
GITHUB_URL = re.compile(r"github\.com[:/]([^/]+)/([^/]+?)(?:\.git)?/?$")


class GhError(Exception):
    """GitHub could not be asked"""


def branches_pushed(stdin):
    """The remote branches a push writes to, without deletions, tags and the long-lived branches"""
    branches = []
    for line in stdin:
        parts = line.split()
        if len(parts) != 4:
            continue
        _local_ref, local_sha, remote_ref, _remote_sha = parts
        if local_sha == ZERO or not remote_ref.startswith("refs/heads/"):
            continue
        branch = remote_ref[len("refs/heads/"):]
        if branch not in LONG_LIVED:
            branches.append(branch)
    return branches


def repository_of(url):
    """owner/name of a GitHub remote URL, or None for a remote that is not GitHub"""
    match = GITHUB_URL.search(url)
    return f"{match.group(1)}/{match.group(2)}" if match else None


def verdict(branch, pulls):
    """None if the push may go through, else the message that refuses it"""
    if not pulls or any(pull["state"] == "open" for pull in pulls):
        return None
    newest = max(pulls, key=lambda pull: pull["number"])
    how = "merged" if newest.get("merged_at") else "closed"
    return (f"pre-push: refused, {branch} is the branch of #{newest['number']}, which is {how}. A push "
            f"after the merge is lost, and it makes the branch anew. Branch anew from develop instead "
            f"(AG-061): git checkout --no-track -b <new branch> origin/develop")


def github_pulls(repository, owner, branch):
    """The pull requests whose head is the branch, open and closed, through gh"""
    result = subprocess.run(["gh", "api", f"repos/{repository}/pulls?head={owner}:{branch}&state=all&per_page=100"],
                            capture_output=True, text=True)
    if result.returncode:
        raise GhError((result.stderr or result.stdout).strip()[:300])
    return [{"number": pull["number"], "state": pull["state"], "merged_at": pull.get("merged_at")}
            for pull in json.loads(result.stdout)]


def main(argv, stdin, out, pulls_of=github_pulls):
    repository = repository_of(argv[2]) if len(argv) > 2 else None
    branches = branches_pushed(stdin)
    if repository is None or not branches:
        return 0
    owner = repository.split("/")[0]
    refused = False
    for branch in branches:
        try:
            message = verdict(branch, pulls_of(repository, owner, branch))
        except GhError as failure:
            message = (f"pre-push: refused, GitHub could not be asked about the pull requests of {branch}: "
                       f"{failure}. Push again once gh answers")
        if message:
            print(message, file=out)
            refused = True
    return 1 if refused else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv, sys.stdin, sys.stderr))
