# PR-PFLICHT

Branches, pushing and pull requests follow the family rules in the profile repository,
[rules/core/git-and-prs.md](https://github.com/astrapi69/astrapi69/blob/main/rules/core/git-and-prs.md):
AG-056 (gitflow), AG-060 (when to push), AG-068 and AG-069 (citing and closing an issue), AG-073 (a pull request with
every push), AG-074 (the cases without one), AG-075 (one concern per pull request) and NW-026 (the squash pilot, which
holds for this repository). They are not repeated here, so a copy cannot drift from them.

## What only this repository adds

- **Hotfix branches** ([RL-093](https://github.com/astrapi69/astrapi69/blob/main/rules/releases.md)): a pull request
  that patches a release goes against `hotfix/X.Y.Z` and is merged with `make merge-pr PR=n TARGET=hotfix/X.Y.Z`; the
  script refuses a pull request whose base is not the target it was given. The form of a hotfix is in
  release-workflow.md (#395).
- **The squash pilot** is enforced by `scripts/merge_pr.py` (`make merge-pr`): it refuses a title that is not a
  Conventional Commit or contains `(#`, and a feat, fix, perf or breaking pull request without its `changelog.d/`
  fragment. Auto-merge runs none of these checks: whoever turns it on checks them first, and gives the squash subject
  and body (github-issue-policy.md).
- **Required status checks on develop:** `closing-references` refuses a pull request that would close an issue no
  closing line names (lessons-learned.md), and `release-freeze` is red on every other pull request while a release
  pull request is open, except one labelled `release-content` (vibe-coding.md).
